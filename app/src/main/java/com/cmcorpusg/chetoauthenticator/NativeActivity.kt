package com.cmcorpusg.chetoauthenticator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.cmcorpusg.chetoauthenticator.backup.DriveBackupClient
import com.cmcorpusg.chetoauthenticator.backup.RecoveryKeyStore
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings
import com.cmcorpusg.chetoauthenticator.backup.BackupScheduler
import com.cmcorpusg.chetoauthenticator.core.OtpAuthParser
import com.cmcorpusg.chetoauthenticator.core.TotpEngine
import com.cmcorpusg.chetoauthenticator.data.*
import com.cmcorpusg.chetoauthenticator.ui.NativeApp
import com.cmcorpusg.chetoauthenticator.ui.ServiceCatalog
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class NativeActivity : FragmentActivity() {
    private lateinit var store: NativeVault
    private var vault by mutableStateOf<MobileVault?>(null)
    private var exists by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var scanned by mutableStateOf<MobileAccount?>(null)
    private var external = false
    private var pendingExport: String? = null
    private var pendingPassword = ""
    private var pendingDrive: ((String) -> Unit)? = null
    private var photoAccount: String? = null
    private var stagedPhoto by mutableStateOf<String?>(null)
    private var biometricReady by mutableStateOf(false)
    private var autoLockJob: Job? = null
    private var backgroundAtMillis: Long = 0L
    private var pendingBiometricEnable = false
    private var lastCopiedCode: String? = null
    private val enrollBiometric = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        external=false
        if(pendingBiometricEnable){
            pendingBiometricEnable=false
            refreshBiometricReady()
            if(biometricReady) confirmBiometricEnrollment()
            else message("No se registró una biometría compatible. Puedes seguir usando tu PIN.")
        }
    }
    private val saveFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        external=false
        val payload=pendingExport; pendingExport=null
        if(uri!=null&&payload!=null) work("Copia cifrada guardada") {
            contentResolver.openOutputStream(uri,"wt")!!.use { it.write(payload.toByteArray()) }
        }
    }
    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        external=false
        val password=pendingPassword;pendingPassword=""
        val current=vault
        if(uri!=null&&current!=null) work("Copia restaurada") {
            val bytes=contentResolver.openInputStream(uri)!!.use { input ->
                val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=16_000_000){"Archivo demasiado grande"};out.write(buffer,0,n)}
                out.toByteArray()
            }
            val restored=NativeVault.restore(bytes.decodeToString(),password,current)
            store.write(restored);withContext(Dispatchers.Main){if(vault!=null)vault=restored}
        }
    }
    private val pickQr = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        external=false
        if(uri!=null){
            val reader=BarcodeScanning.getClient()
            runCatching { InputImage.fromFilePath(this,uri) }.onSuccess { image ->
                reader.process(image).addOnSuccessListener { codes ->
                    val raw=codes.firstOrNull { it.rawValue?.startsWith("otpauth://")==true }?.rawValue
                    if(raw==null)message("No se encontró un QR TOTP") else receiveQr(raw)
                }.addOnFailureListener { message("No se pudo leer la imagen") }.addOnCompleteListener { reader.close() }
            }.onFailure { reader.close();message("Imagen inválida") }
        }
    }
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        external=false
        if(uri!=null) work("Imagen guardada") {
            val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it,null,options) }
            require(options.outWidth>0&&options.outHeight>0)
            options.inJustDecodeBounds=false;options.inSampleSize=(maxOf(options.outWidth,options.outHeight)/256).coerceAtLeast(1)
            val image=contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it,null,options) } ?: error("Imagen inválida")
            val small=Bitmap.createScaledBitmap(image,256,256,true)
            val data=ByteArrayOutputStream().apply { small.compress(Bitmap.CompressFormat.PNG,100,this) }.toByteArray()
            val photo="data:image/png;base64,"+android.util.Base64.encodeToString(data,android.util.Base64.NO_WRAP)
            withContext(Dispatchers.Main) {
                if(photoAccount==null)vault?.let { update(it.copy(photo=photo)) } else stagedPhoto=photo
                photoAccount=null
            }
        }
    }
    private val authorize = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        external=false
        val action=pendingDrive;pendingDrive=null
        if(result.resultCode==RESULT_OK&&result.data!=null) runCatching {
            val token=Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data!!).accessToken
            require(!token.isNullOrBlank());action?.invoke(token)
        }.onFailure { message("No se pudo autorizar Google Drive") }
        else { pendingExport=null;pendingPassword="";message("Autorización cancelada") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        store=NativeVault(this);exists=store.exists();refreshBiometricReady()
        setContent {
            NativeApp(vault,exists,busy,scanned,stagedPhoto,biometricReady,
                onLogin={ pin -> runCatching { store.unlock(pin) }.onSuccess { vault=it;applySettings(it) }.onFailure { message(it.message?:"No se pudo abrir el perfil") } },
                onRegister={ name,email,pin ->
                    if(!store.exists())runCatching {
                        val initial=MobileVault(pin=pin,name=name,emails=listOf(email),accounts=store.legacyAccounts())
                        store.write(initial)
                        store.read()
                    }.onSuccess { persisted->vault=persisted;exists=true }
                        .onFailure { message("No se pudo guardar el perfil") }
                },onBiometric=::biometric,onBiometricSetup=::requestBiometricSetup,onVerifyPin=::verifyPin,onVerifyBiometric=::reauthenticateBiometric,onChangePin=::changePin,onUpdate=::update,
                onScan={ photo -> external=true;if(photo)pickQr.launch("image/*") else GmsBarcodeScanning.getClient(this).startScan()
                    .addOnSuccessListener { it.rawValue?.let(::receiveQr) }.addOnFailureListener { message("Escáner no disponible. Usa una imagen o la clave manual.") }
                    .addOnCompleteListener { external=false } },
                onScannedConsumed={scanned=null},onCopy=::copyCode,onBackup=::backup,
                onPhoto={ account -> photoAccount=account;external=true;pickPhoto.launch("image/*") },
                onPhotoConsumed={stagedPhoto=null},onLock=::lockNow,onMessage=::message)
        }
    }
    override fun onResume(){
        super.onResume()
        if(::store.isInitialized)refreshBiometricReady()
        autoLockJob?.cancel()
        autoLockJob=null
        val current=vault
        if(!external && current!=null && backgroundAtMillis>0L && current.lockTimeoutSeconds>0){
            val elapsed=System.currentTimeMillis()-backgroundAtMillis
            if(elapsed>=current.lockTimeoutSeconds*1000L) lockNow()
        }
        backgroundAtMillis=0L
        vault?.let(::applySettings)
    }
    override fun onStop(){
        super.onStop()
        if(!external){
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            clearSensitiveClipboard()
            scanned=null
            stagedPhoto=null
            val current=vault ?: return
            backgroundAtMillis=System.currentTimeMillis()
            autoLockJob?.cancel()
            if(current.lockTimeoutSeconds<=0){
                lockNow()
            }else{
                autoLockJob=lifecycleScope.launch {
                    delay(current.lockTimeoutSeconds*1000L)
                    lockNow()
                }
            }
        }
    }
    private fun message(value: String){Toast.makeText(this,value,Toast.LENGTH_LONG).show()}
    private fun lockNow(){
        autoLockJob?.cancel()
        autoLockJob=null
        backgroundAtMillis=0L
        vault=null
        scanned=null
        stagedPhoto=null
        clearSensitiveClipboard()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    private fun applySettings(s: MobileVault){if(s.screenshots)window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)}
    private fun update(s: MobileVault){
        runCatching {
            store.write(s)
            store.read()
        }.onSuccess { persisted->
            vault=persisted
            applySettings(persisted)
            if(BackupSettings(this).driveEnabled){
                BackupScheduler.runNow(this)
            }
        }.onFailure { message("No se pudieron guardar los cambios") }
    }
    private fun verifyPin(pin:String):Boolean =
        runCatching {
            store.unlock(pin)
            true
        }.getOrElse {
            message(it.message ?: "PIN incorrecto")
            false
        }

    private fun reauthenticateBiometric(onSuccess:()->Unit){
        val state=runCatching { store.read() }.getOrNull()
        if(state?.biometric!=true || !biometricReady){
            message("Usa tu PIN para confirmar esta acción")
            return
        }
        external=true
        BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){
                external=false
                onSuccess()
            }
            override fun onAuthenticationError(code:Int,text:CharSequence){
                external=false
                message("Acción no confirmada")
            }
            override fun onAuthenticationFailed(){message("Huella no reconocida")}
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirmar acción")
                .setSubtitle("Verifica tu identidad para continuar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Usar PIN")
                .build()
        )
    }

    private fun changePin(currentPin:String,newPin:String){
        runCatching {
            val verified=store.unlock(currentPin)
            store.write(verified.copy(pin=newPin))
            store.read()
        }.onSuccess { persisted->
            vault=persisted
            applySettings(persisted)
            message("PIN actualizado")
        }.onFailure { message(it.message ?: "No se pudo cambiar el PIN") }
    }

    private fun work(success: String,action: suspend ()->Unit){
        busy=true;lifecycleScope.launch {
            try{withContext(Dispatchers.IO){action()};message(success)}catch(e:Exception){message("No se pudo completar. Verifica el archivo, la contraseña o la conexión.")}
            finally{busy=false}
        }
    }
    private fun receiveQr(raw:String){
        if(vault==null)return
        runCatching { OtpAuthParser.parse(raw).also { TotpEngine.generate(it.secret,digits=it.digits,period=it.period,algorithm=it.algorithm) } }
            .onSuccess {
                val candidate=MobileAccount(
                    issuer=it.issuer,
                    label=it.label,
                    secret=it.secret,
                    digits=it.digits,
                    period=it.period,
                    algorithm=it.algorithm,
                    photo=ServiceCatalog.logoUrlFor(it.issuer).orEmpty()
                )
                val duplicate=AccountPolicy.findDuplicate(candidate,vault?.accounts.orEmpty())
                if(duplicate!=null) message("Esta cuenta ya existe en CHETO")
                else scanned=candidate
            }
            .onFailure { message("QR TOTP inválido") }
    }
    private fun copyCode(value:String){
        val clip=ClipData.newPlainText("Código 2FA",value)
        clip.description.extras=android.os.PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE",true) }
        val clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)
        lastCopiedCode=value
        message("Código copiado")
        lifecycleScope.launch {
            delay(30_000)
            if(clipboard.primaryClip?.getItemAt(0)?.text?.toString()==value){
                clipboard.setPrimaryClip(ClipData.newPlainText("",""))
            }
            if(lastCopiedCode==value) lastCopiedCode=null
        }
    }

    private fun clearSensitiveClipboard(){
        val expected=lastCopiedCode ?: return
        val clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        if(clipboard.primaryClip?.getItemAt(0)?.text?.toString()==expected){
            clipboard.setPrimaryClip(ClipData.newPlainText("",""))
        }
        lastCopiedCode=null
    }
    private fun refreshBiometricReady(){
        biometricReady=BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)==BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun requestBiometricSetup(){
        when(BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)){
            BiometricManager.BIOMETRIC_SUCCESS -> confirmBiometricEnrollment()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> launchBiometricEnrollment()
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> message("Este dispositivo no tiene biometría compatible.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> message("El sensor biométrico no está disponible ahora. Intenta nuevamente.")
            else -> message("Android no permite usar biometría en este momento. Puedes continuar con tu PIN.")
        }
    }

    private fun launchBiometricEnrollment(){
        val intent=when{
            Build.VERSION.SDK_INT>=Build.VERSION_CODES.R -> Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
            }
            Build.VERSION.SDK_INT>=Build.VERSION_CODES.P -> Intent(Settings.ACTION_FINGERPRINT_ENROLL)
            else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        if(intent.resolveActivity(packageManager)==null){
            message("Abre Ajustes de Android y registra una huella para continuar.")
            return
        }
        pendingBiometricEnable=true
        external=true
        enrollBiometric.launch(intent)
    }

    private fun confirmBiometricEnrollment(){
        external=true
        BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){
                external=false
                biometricReady=true
                runCatching {
                    val current=store.read()
                    store.write(current.copy(biometric=true))
                    store.read()
                }.onSuccess { persisted->
                    vault=persisted
                    applySettings(persisted)
                    message("Biometría activada en CHETO")
                }.onFailure { message("No se pudo activar la biometría") }
            }
            override fun onAuthenticationError(code:Int,text:CharSequence){
                external=false
                refreshBiometricReady()
                message("Biometría no activada. Puedes continuar con tu PIN.")
            }
            override fun onAuthenticationFailed(){
                message("Huella no reconocida. Intenta nuevamente.")
            }
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Activar biometría en CHETO")
                .setSubtitle("Confirma tu huella para habilitar el acceso biométrico")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancelar")
                .build()
        )
    }

    private fun biometric(){
        if(!exists)return
        val enabled=runCatching { store.read().biometric }.getOrDefault(false)
        if(!enabled){
            requestBiometricSetup()
            return
        }
        when(BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)){
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                requestBiometricSetup()
                return
            }
            else -> {
                message("La biometría no está disponible. Usa tu PIN.")
                return
            }
        }
        external=true
        BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){
                external=false
                runCatching { store.read() }
                    .onSuccess { vault=it;applySettings(it) }
                    .onFailure { message("No se pudo abrir el perfil") }
            }
            override fun onAuthenticationError(code:Int,text:CharSequence){external=false;message("Puedes entrar con tu PIN")}
            override fun onAuthenticationFailed(){message("Huella no reconocida")}
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("CHETO Authenticator")
                .setSubtitle("Confirma tu identidad")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Usar PIN")
                .build()
        )
    }
    private fun drive(action:(String)->Unit){
        val request=AuthorizationRequest.builder().setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.appdata"))).build()
        Identity.getAuthorizationClient(this).authorize(request).addOnSuccessListener { result ->
            if(result.hasResolution()){
                val intent=result.pendingIntent
                if(intent==null)message("Google Drive requiere configuración OAuth") else {pendingDrive=action;external=true;authorize.launch(IntentSenderRequest.Builder(intent.intentSender).build())}
            }else result.accessToken?.let(action) ?: message("Google no devolvió acceso")
        }.addOnFailureListener { pendingExport=null;pendingPassword="";message("Configura OAuth Android y el SHA-1 de este APK para conectar Drive. Puedes usar el respaldo local.") }
    }
    private fun backup(mode:String,password:String){
        val current=vault?:return
        when(mode){
            "restore"->{pendingPassword=password;external=true;openFile.launch(arrayOf("*/*"))}
            "driveRestore"->drive { token -> work("Copia de Drive restaurada") {
                val data=DriveBackupClient("cheto_native_backup_").downloadLatest(token)?:error("Sin copias")
                val restored=NativeVault.restore(data,password,current);store.write(restored)
                withContext(Dispatchers.Main){if(vault!=null)vault=restored}
            } }
            else->work(if(mode=="export")"Elige dónde guardar la copia" else "Copia preparada") {
                val payload=NativeVault.export(current,password)
                withContext(Dispatchers.Main){
                    if(mode=="export"){
                        pendingExport=payload
                        external=true
                        saveFile.launch("CHETO-${System.currentTimeMillis()}.cheto")
                    } else drive { token ->
                        work("Copia cifrada guardada en Google Drive") {
                            DriveBackupClient("cheto_native_backup_").upload(token,payload)
                            RecoveryKeyStore(this@NativeActivity).configure(password.toCharArray())
                            BackupSettings(this@NativeActivity).apply {
                                driveEnabled=true
                                lastBackupEpochMillis=System.currentTimeMillis()
                                lastError=null
                            }
                            BackupScheduler.schedule(this@NativeActivity)
                        }
                    }
                }
            }
        }
    }
}
