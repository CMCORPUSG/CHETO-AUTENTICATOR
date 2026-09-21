package com.cmcorpusg.chetoauthenticator.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmcorpusg.chetoauthenticator.backup.BackupScheduler
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings
import com.cmcorpusg.chetoauthenticator.core.TotpEngine
import com.cmcorpusg.chetoauthenticator.data.AccountPolicy
import com.cmcorpusg.chetoauthenticator.data.MobileAccount
import com.cmcorpusg.chetoauthenticator.data.MobileVault
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private val Blue=ChetoBlue
private val Purple=ChetoViolet

private data class PendingCriticalAction(
    val title:String,
    val description:String,
    val action:()->Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeApp(
    vault:MobileVault?,exists:Boolean,busy:Boolean,scanned:MobileAccount?,stagedPhoto:String?,biometricReady:Boolean,
    onLogin:(String)->Unit,onRegister:(String,String,String)->Unit,onBiometric:()->Unit,onBiometricSetup:()->Unit,
    onVerifyPin:(String)->Boolean,onVerifyBiometric:((()->Unit))->Unit,
    onChangePin:(String,String)->Unit,onUpdate:(MobileVault)->Unit,onScan:(Boolean)->Unit,onScannedConsumed:()->Unit,onCopy:(String)->Unit,
    onBackup:(String,String)->Unit,onDisableBackup:()->Unit,
    googleIdentityConfigured:Boolean,microsoftIdentityConfigured:Boolean,
    onLinkGoogle:()->Unit,onLinkMicrosoft:()->Unit,onUnlinkIdentity:(String,String)->Unit,
    onPhoto:(String?)->Unit,onPhotoConsumed:()->Unit,onLock:()->Unit,onMessage:(String)->Unit
){
    ChetoTheme(dark=vault?.dark==true){
        Surface(Modifier.fillMaxSize()){
            if(vault==null){LoginScreen(exists,onLogin,onRegister,onBiometric,onMessage);return@Surface}
            var page by remember { mutableStateOf("Inicio") }
            var editor by remember { mutableStateOf<MobileAccount?>(null) }
            var addAccount by remember { mutableStateOf(false) }
            var delete by remember { mutableStateOf<MobileAccount?>(null) }
            var backupMode by remember { mutableStateOf<String?>(null) }
            var changePin by remember { mutableStateOf(false) }
            var manageCategories by remember { mutableStateOf(false) }
            var securityCenter by remember { mutableStateOf(false) }
            var critical by remember { mutableStateOf<PendingCriticalAction?>(null) }
            var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit){ while(true){ clockMillis=System.currentTimeMillis(); delay(30_000) } }
            LaunchedEffect(scanned){if(scanned!=null){addAccount=false;editor=scanned;onScannedConsumed()}}
            LaunchedEffect(stagedPhoto){if(stagedPhoto!=null&&editor!=null){editor=editor!!.copy(photo=stagedPhoto);onPhotoConsumed()}}
            BackHandler { when {
                editor!=null->editor=null
                addAccount->addAccount=false
                manageCategories->manageCategories=false
                securityCenter->securityCenter=false
                page!="Inicio"->page="Inicio"
                else->onLock()
            } }
            Scaffold(
                topBar={
                    Column(
                        Modifier.background(Brush.linearGradient(listOf(Color(0xFF263EAF),Blue,Purple)))
                            .statusBarsPadding().fillMaxWidth().padding(horizontal=18.dp,vertical=13.dp)
                    ){
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            Surface(shape=RoundedCornerShape(13.dp),color=Color.White.copy(alpha=.15f),modifier=Modifier.size(42.dp)){
                                Box(contentAlignment=Alignment.Center){
                                    Icon(Icons.Rounded.Shield,contentDescription=null,tint=Color.White,modifier=Modifier.size(23.dp))
                                }
                            }
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)){
                                Text(
                                    when {
                                        manageCategories -> "Categorías"
                                        securityCenter -> "Centro de seguridad"
                                        else -> page
                                    },
                                    color=Color.White,
                                    fontSize=19.sp,
                                    fontWeight=FontWeight.Bold
                                )
                                Text("CHETO · Lima · " + LimaClock.nowLabel(clockMillis),color=Color.White.copy(alpha=.72f),style=MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick=onLock){Icon(Icons.Rounded.Lock,contentDescription="Bloquear",tint=Color.White)}
                        }
                    }
                },bottomBar={if(!manageCategories&&!securityCenter)NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=3.dp) {
                    val destinations=listOf(
                        Triple("Inicio",Icons.Rounded.Home,"Inicio"),
                        Triple("Backup",Icons.Rounded.Cloud,"Backup"),
                        Triple("Perfil",Icons.Rounded.Person,"Perfil"),
                        Triple("Ajustes",Icons.Rounded.Settings,"Ajustes")
                    )
                    destinations.forEach { (name,icon,label)->
                        NavigationBarItem(
                            selected=page==name,
                            onClick={page=name},
                            icon={Icon(icon,contentDescription=label)},
                            label={Text(label)}
                        )
                    }
                }},floatingActionButton={if(page=="Inicio"&&!manageCategories&&!securityCenter)FloatingActionButton(onClick={addAccount=true},containerColor=Blue,contentColor=Color.White,shape=RoundedCornerShape(17.dp)){Icon(Icons.Rounded.Add,contentDescription="Agregar cuenta")}}
            ){padding->
                Column(Modifier.padding(padding).fillMaxSize()){
                    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                    when {
                        manageCategories -> CategoryManagerScreen(vault,onUpdate,{manageCategories=false},onMessage)
                        securityCenter -> SecurityCenterScreen(vault,biometricReady,onLock)
                        else -> when(page){
                            "Inicio"->HomeScreen(
                                vault,
                                onCopy,
                                {editor=it},
                                { account->
                                    critical=PendingCriticalAction(
                                        "Eliminar cuenta",
                                        "Confirma tu identidad antes de eliminar una cuenta TOTP."
                                    ){delete=account}
                                },
                                { account->
                                    onUpdate(
                                        vault.copy(
                                            accounts=vault.accounts.map {
                                                if(it.id==account.id) it.copy(favorite=!it.favorite) else it
                                            }
                                        )
                                    )
                                },
                                {manageCategories=true}
                            )
                            "Backup"->BackupPage(
                                busy=busy,
                                action={ mode->
                                    critical=PendingCriticalAction(
                                        "Acceso al respaldo",
                                        "Confirma tu identidad antes de exportar, restaurar o sincronizar la bóveda."
                                    ){backupMode=mode}
                                },
                                onDisableAuto={ after->
                                    critical=PendingCriticalAction(
                                        "Desactivar backup automático",
                                        "Sin backup automático, los cambios futuros dependerán de tus copias manuales."
                                    ){
                                        onDisableBackup()
                                        after()
                                    }
                                }
                            )
                            "Perfil"->UserProfileScreen(
                                vault=vault,
                                onUpdate=onUpdate,
                                onPhoto={onPhoto(null)},
                                googleConfigured=googleIdentityConfigured,
                                microsoftConfigured=microsoftIdentityConfigured,
                                onLinkGoogle=onLinkGoogle,
                                onLinkMicrosoft=onLinkMicrosoft,
                                onUnlinkIdentity={provider,subject->
                                    critical=PendingCriticalAction(
                                        "Desvincular identidad",
                                        "Confirma tu identidad antes de desvincular una cuenta externa de CHETO."
                                    ){onUnlinkIdentity(provider,subject)}
                                },
                                onMessage=onMessage
                            )
                            "Ajustes"->SettingsPage(
                                vault,
                                biometricReady,
                                onUpdate,
                                {manageCategories=true},
                                {changePin=true},
                                onBiometricSetup,
                                {securityCenter=true},
                                {title,action->
                                    critical=PendingCriticalAction(
                                        title,
                                        "Esta opción cambia la protección de CHETO. Confirma tu identidad para continuar.",
                                        action
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if(addAccount)AddAccountScreen(
                onDismiss={addAccount=false},
                onScan={photo->addAccount=false;onScan(photo)},
                onManual={account->addAccount=false;editor=account}
            )
            editor?.let { account->AccountEditor(account,vault.categories,onPhoto={onPhoto(account.id)},onDismiss={editor=null},onSave={a->
                val duplicate=AccountPolicy.findDuplicate(a,vault.accounts)
                if(duplicate!=null){
                    onMessage("Ya existe una cuenta igual: ${duplicate.issuer} · ${duplicate.label}")
                }else{
                    onUpdate(vault.copy(accounts=if(vault.accounts.any { it.id==a.id })vault.accounts.map { if(it.id==a.id)a else it } else vault.accounts+a))
                    editor=null
                }
            },onMessage=onMessage) }
            delete?.let { account->AlertDialog(onDismissRequest={delete=null},title={Text("Eliminar cuenta")},text={Text("¿Eliminar ${account.issuer} (${account.label})? Conserva una copia antes de eliminarla.")},confirmButton={TextButton(onClick={onUpdate(vault.copy(accounts=vault.accounts.filterNot { it.id==account.id }));delete=null}){Text("Eliminar")}},dismissButton={TextButton(onClick={delete=null}){Text("Cancelar")}}) }
            backupMode?.let { mode->PasswordDialog(
                title=if(mode.contains("estore"))"Restaurar copia" else "Crear copia cifrada",
                description=if(mode.contains("estore"))"Reemplazará las cuentas y el perfil actuales. Introduce la contraseña de la copia." else "Usa al menos 10 caracteres. Guarda esta contraseña: la necesitarás para recuperar tus cuentas.",
                onDismiss={backupMode=null},onConfirm={p->if(p.isBlank()||(!mode.contains("estore")&&p.length<10))onMessage("Revisa la contraseña") else {backupMode=null;onBackup(mode,p)}}) }
            if(changePin)ChangePinDialog(
                onDismiss={changePin=false},
                onConfirm={currentPin,newPin->
                    onChangePin(currentPin,newPin)
                    changePin=false
                },
                onMessage=onMessage
            )
            critical?.let { pending->
                CriticalActionDialog(
                    title=pending.title,
                    description=pending.description,
                    biometricAvailable=vault.biometric&&biometricReady,
                    onDismiss={critical=null},
                    onVerifyPin=onVerifyPin,
                    onVerifyBiometric=onVerifyBiometric,
                    onConfirmed={
                        val action=pending.action
                        critical=null
                        action()
                    },
                    onMessage=onMessage
                )
            }
        }
    }
}

@Composable private fun LoginScreen(
    exists:Boolean,
    onLogin:(String)->Unit,
    onRegister:(String,String,String)->Unit,
    onBio:()->Unit,
    onMessage:(String)->Unit
){
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF172968),Color(0xFF314FD5),Purple)))){
        Box(Modifier.size(260.dp).offset(x=190.dp,y=(-80).dp).background(Color.White.copy(alpha=.05f),CircleShape))
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=24.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center
        ){
            Surface(modifier=Modifier.size(66.dp),shape=RoundedCornerShape(21.dp),color=Color.White.copy(alpha=.13f)){
                Box(contentAlignment=Alignment.Center){Icon(Icons.Rounded.Shield,contentDescription=null,tint=Color.White,modifier=Modifier.size(34.dp))}
            }
            Spacer(Modifier.height(12.dp))
            Text("CHETO",color=Color.White,style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Black)
            Text(if(exists)"Tu autenticador, protegido" else "Configura tu bóveda segura",color=Color.White.copy(alpha=.76f),style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(22.dp))

            Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(10.dp)){
                Column(Modifier.padding(horizontal=20.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(13.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
                        Text(if(exists)"Hola de nuevo" else "Crea tu perfil",style=MaterialTheme.typography.headlineSmall)
                        Text(if(exists)"Ingresa tu PIN de 6 dígitos" else "Solo toma un minuto",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if(exists){
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            repeat(6){ index->
                                Box(Modifier.size(if(index<pin.length)11.dp else 10.dp).clip(CircleShape).background(if(index<pin.length)Blue else MaterialTheme.colorScheme.outlineVariant))
                            }
                        }
                        PinPad(pin,{pin=it}){
                            if(pin.length==6){onLogin(pin);pin=""}else onMessage("Completa los 6 dígitos")
                        }
                        FilledTonalButton(onClick=onBio,modifier=Modifier.fillMaxWidth().height(44.dp),shape=ControlShape){
                            Icon(Icons.Rounded.Fingerprint,contentDescription=null,modifier=Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text("Usar biometría")
                        }
                    }else{
                        Field("Nombre",name,{name=it})
                        Field("Correo",email,{email=it},keyboard=KeyboardType.Email)
                        Field("PIN de 6 dígitos",pin,{pin=it.filter(Char::isDigit).take(6)},password=true,keyboard=KeyboardType.NumberPassword)
                        Field("Confirmar PIN",confirm,{confirm=it.filter(Char::isDigit).take(6)},password=true,keyboard=KeyboardType.NumberPassword)
                        Button(onClick={
                            when{
                                name.isBlank()->onMessage("Escribe tu nombre")
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()->onMessage("Introduce un correo válido")
                                pin.length!=6->onMessage("El PIN debe tener 6 dígitos")
                                pin!=confirm->onMessage("Los PIN no coinciden")
                                else->onRegister(name.trim(),email.trim(),pin)
                            }
                        },modifier=Modifier.fillMaxWidth().height(46.dp),shape=ControlShape){Text("Crear bóveda")}
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        Icon(Icons.Rounded.Lock,contentDescription=null,modifier=Modifier.size(14.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Cifrado local · funciona sin internet",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun PinPad(
    pin:String,
    onPinChange:(String)->Unit,
    onSubmit:()->Unit
){
    val rows=listOf(
        listOf("1","2","3"),
        listOf("4","5","6"),
        listOf("7","8","9"),
        listOf("⌫","0","✓")
    )
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        rows.forEach { row->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                row.forEach { key->
                    FilledTonalButton(
                        onClick={
                            when(key){
                                "⌫" -> if(pin.isNotEmpty())onPinChange(pin.dropLast(1))
                                "✓" -> onSubmit()
                                else -> if(pin.length<6)onPinChange(pin+key)
                            }
                        },
                        modifier=Modifier.weight(1f).height(48.dp),
                        shape=RoundedCornerShape(15.dp),
                        contentPadding=PaddingValues(0.dp)
                    ){
                        Text(key,fontSize=18.sp,fontWeight=FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable private fun Accounts(
    v:MobileVault,
    onCopy:(String)->Unit,
    onEdit:(MobileAccount)->Unit,
    onDelete:(MobileAccount)->Unit,
    onScan:(Boolean)->Unit,
    onCategories:()->Unit
){
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()/1000) }
    var revealed by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit){while(true){now=System.currentTimeMillis()/1000;delay(1000)}}
    LaunchedEffect(revealed){if(revealed!=null){delay(10000);revealed=null}}

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(16.dp,16.dp,16.dp,96.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(10.dp)
            ){
                MetricCard(
                    title="Cuentas",
                    value=v.accounts.size.toString(),
                    modifier=Modifier.weight(1f)
                )
                MetricCard(
                    title="Categorías",
                    value=v.categories.size.toString(),
                    modifier=Modifier.weight(1f)
                )
            }
        }

        item{
            Card(
                colors=CardDefaults.cardColors(
                    containerColor=MaterialTheme.colorScheme.primaryContainer
                ),
                shape=RoundedCornerShape(22.dp)
            ){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Text("Tu bóveda 2FA",fontWeight=FontWeight.Bold,fontSize=18.sp)
                    Text(
                        "Escanea un QR, importa desde una imagen o agrega la clave manualmente.",
                        style=MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item{Field("Buscar cuentas",search,{search=it})}

        item{
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                (listOf("Todos")+v.categories).forEach { c->
                    val color=categoryColor(c,v.categoryColors)
                    FilterChip(
                        selected=c==category,
                        onClick={category=c},
                        label={Text(c)},
                        colors=FilterChipDefaults.filterChipColors(
                            containerColor=color.copy(alpha=.10f),
                            selectedContainerColor=color.copy(alpha=.24f)
                        )
                    )
                }
                AssistChip(onClick=onCategories,label={Text("+ Categorías")})
            }
        }

        item{
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilledTonalButton(
                    onClick={onScan(false)},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Rounded.QrCodeScanner,contentDescription=null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cámara")
                }
                FilledTonalButton(
                    onClick={onScan(true)},
                    modifier=Modifier.weight(1f)
                ){
                    Icon(Icons.Rounded.PhotoLibrary,contentDescription=null)
                    Spacer(Modifier.width(6.dp))
                    Text("Imagen")
                }
            }
        }

        val accounts=v.accounts.filter {
            (category=="Todos"||it.category==category) &&
                (it.issuer+" "+it.label).contains(search,true)
        }

        if(accounts.isEmpty())item{
            Panel(
                if(search.isBlank())"Aún no hay cuentas" else "Sin resultados",
                if(search.isBlank())
                    "Toca + para agregar una cuenta o usa el escáner QR."
                else "Prueba otro nombre, correo o categoría."
            )
        }

        items(accounts,key={it.id}){a->
            val code=remember(a,now/a.period){
                runCatching {
                    TotpEngine.generate(a.secret,now,a.digits,a.period,a.algorithm)
                }.getOrDefault("------")
            }
            val seconds=TotpEngine.secondsRemaining(now,a.period)
            val accent=categoryColor(a.category,v.categoryColors)

            Card(
                shape=RoundedCornerShape(24.dp),
                colors=CardDefaults.cardColors(
                    containerColor=MaterialTheme.colorScheme.surface
                ),
                elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
            ){
                Column(
                    Modifier.padding(17.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    Row(
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(12.dp)
                    ){
                        Avatar(a.issuer,a.photo)
                        Column(Modifier.weight(1f)){
                            Text(a.issuer,fontWeight=FontWeight.Bold,fontSize=17.sp)
                            Text(a.label,style=MaterialTheme.typography.bodySmall)
                            Surface(
                                shape=RoundedCornerShape(50),
                                color=accent.copy(alpha=.14f)
                            ){
                                Text(
                                    a.category,
                                    modifier=Modifier.padding(horizontal=9.dp,vertical=3.dp),
                                    style=MaterialTheme.typography.labelSmall,
                                    color=accent
                                )
                            }
                        }
                    }

                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(
                            if(v.hideCodes&&revealed!=a.id)"••• •••"
                            else code.chunked(if(a.digits==6)3 else 4).joinToString(" "),
                            fontSize=31.sp,
                            fontWeight=FontWeight.Black,
                            letterSpacing=1.sp,
                            modifier=Modifier.weight(1f).clickable {
                                if(v.hideCodes)revealed=a.id else onCopy(code)
                            }
                        )
                        Box(contentAlignment=Alignment.Center){
                            CircularProgressIndicator(
                                progress={seconds.toFloat()/a.period},
                                modifier=Modifier.size(46.dp),
                                strokeWidth=4.dp
                            )
                            Text("${seconds}s",fontSize=11.sp)
                        }
                    }

                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        TextButton(onClick={onCopy(code)}){
                            Icon(Icons.Rounded.ContentCopy,contentDescription=null)
                            Spacer(Modifier.width(4.dp))
                            Text("Copiar")
                        }
                        TextButton(onClick={onEdit(a)}){
                            Icon(Icons.Rounded.Edit,contentDescription=null)
                            Spacer(Modifier.width(4.dp))
                            Text("Editar")
                        }
                        TextButton(onClick={onDelete(a)}){
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription=null,
                                tint=MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar",color=MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun MetricCard(
    title:String,
    value:String,
    modifier:Modifier=Modifier
){
    Card(
        modifier=modifier,
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(
            containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)
        )
    ){
        Column(Modifier.padding(16.dp)){
            Text(value,fontSize=26.sp,fontWeight=FontWeight.Black)
            Text(title,style=MaterialTheme.typography.bodySmall)
        }
    }
}

internal val categoryPalette=linkedMapOf(
    "#3157F6" to Color(0xFF3157F6),
    "#7C4DFF" to Color(0xFF7C4DFF),
    "#00897B" to Color(0xFF00897B),
    "#EF6C00" to Color(0xFFEF6C00),
    "#D81B60" to Color(0xFFD81B60),
    "#546E7A" to Color(0xFF546E7A)
)

internal fun categoryColorHex(name:String,overrides:Map<String,String>):String{
    overrides[name]?.let { if(categoryPalette.containsKey(it.uppercase()))return it.uppercase() }
    val keys=categoryPalette.keys.toList()
    return keys[(name.hashCode() and Int.MAX_VALUE)%keys.size]
}

internal fun categoryColor(name:String,overrides:Map<String,String>):Color =
    categoryPalette[categoryColorHex(name,overrides)] ?: Blue

@Composable private fun AccountEditor(initial:MobileAccount,categories:List<String>,onPhoto:()->Unit,onDismiss:()->Unit,onSave:(MobileAccount)->Unit,onMessage:(String)->Unit){
    var a by remember(initial.id){mutableStateOf(initial)}
    var period by remember(initial.id){mutableStateOf(initial.period.toString())}
    var logoDomain by remember(initial.id){mutableStateOf(ServiceCatalog.domainFor(initial.issuer).orEmpty())}
    LaunchedEffect(initial.photo){a=a.copy(photo=initial.photo)}
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
            Column(Modifier.safeDrawingPadding().imePadding()){
                Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                    IconButton(onClick=onDismiss){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Volver")}
                    Column{
                        Text(if(initial.issuer.isBlank())"Cuenta manual" else "Editar cuenta",style=MaterialTheme.typography.headlineSmall)
                        Text("Identidad y configuración TOTP",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
                    Card(Modifier.fillMaxWidth(),shape=CardShape,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
                            Avatar(a.issuer.ifBlank{"CH"},a.photo,60)
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                                Text(a.issuer.ifBlank{"Nuevo servicio"},style=MaterialTheme.typography.titleLarge)
                                Text(a.label.ifBlank{"Configura la identidad de la cuenta"},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                                if(a.photo.isNotBlank())Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                    Icon(Icons.Rounded.CheckCircle,null,Modifier.size(14.dp),tint=ChetoSuccess)
                                    Text("Logo listo",style=MaterialTheme.typography.labelSmall,color=ChetoSuccess)
                                }
                            }
                        }
                    }

                    SectionHeader("Servicio y logo")
                    Field("Nombre del servicio",a.issuer,{value->a=a.copy(issuer=value);if(logoDomain.isBlank())logoDomain=ServiceCatalog.domainFor(value).orEmpty()})
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        ServiceCatalog.suggestions.take(10).forEach{service->
                            AssistChip(onClick={a=a.copy(issuer=service,photo=ServiceCatalog.logoUrlFor(service).orEmpty());logoDomain=ServiceCatalog.domainFor(service).orEmpty()},label={Text(service)})
                        }
                    }
                    PremiumCard(Modifier.fillMaxWidth()){
                        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                            Field("Dominio del logo",logoDomain,{logoDomain=it.trim()})
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                OutlinedButton(onClick={
                                    val url=ServiceCatalog.logoUrlFor(logoDomain.ifBlank{a.issuer})
                                    if(url==null)onMessage("Escribe un servicio o dominio válido")else{a=a.copy(photo=url);onMessage("Logo online seleccionado")}
                                },modifier=Modifier.weight(1f).height(42.dp),shape=ControlShape,contentPadding=PaddingValues(horizontal=8.dp)){Text("Buscar logo online",maxLines=1,style=MaterialTheme.typography.labelMedium)}
                                OutlinedButton(onClick=onPhoto,modifier=Modifier.weight(1f).height(42.dp),shape=ControlShape,contentPadding=PaddingValues(horizontal=8.dp)){Text("Subir logo",maxLines=1)}
                            }
                        }
                    }

                    SectionHeader("Datos de la cuenta")
                    Field("Cuenta o correo",a.label,{a=a.copy(label=it)})
                    Field("Clave secreta Base32",a.secret,{a=a.copy(secret=it)},password=true)
                    Choice("Categoría",a.category,categories){a=a.copy(category=it)}
                    Field("Notas (opcional)",a.notes,{a=a.copy(notes=it)},singleLine=false)

                    SectionHeader("Configuración avanzada")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Box(Modifier.weight(1f)){Choice("Dígitos",a.digits.toString(),listOf("6","7","8")){a=a.copy(digits=it.toInt())}}
                        Box(Modifier.weight(1f)){Choice("Algoritmo",a.algorithm,listOf("SHA1","SHA256","SHA512")){a=a.copy(algorithm=it)}}
                    }
                    Field("Periodo (segundos)",period,{period=it.filter(Char::isDigit).take(3)},keyboard=KeyboardType.Number)
                    Spacer(Modifier.height(2.dp))
                    Button(onClick={
                        val normalized=a.secret.uppercase().replace(Regex("[\\s=-]"),"")
                        val p=period.toIntOrNull()
                        if(p==null||p !in 1..300||a.issuer.isBlank())onMessage("Completa el servicio y un periodo entre 1 y 300")
                        else runCatching{TotpEngine.generate(normalized,digits=a.digits,period=p,algorithm=a.algorithm)}.onSuccess{
                            val issuer=a.issuer.trim();onSave(a.copy(secret=normalized,period=p,issuer=issuer,label=a.label.trim().ifBlank{"Sin etiqueta"},photo=a.photo.ifBlank{ServiceCatalog.logoUrlFor(issuer).orEmpty()}))
                        }.onFailure{onMessage("Clave Base32 inválida. Copia la clave del servicio.")}
                    },modifier=Modifier.fillMaxWidth().height(46.dp),shape=ControlShape){Text("Guardar cuenta")}
                    OutlinedButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth().height(44.dp),shape=ControlShape){Text("Cancelar")}
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable private fun Categories(
    v:MobileVault,
    onUpdate:(MobileVault)->Unit,
    onBack:()->Unit,
    onMessage:(String)->Unit
){
    var text by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var selectedHex by remember { mutableStateOf(categoryPalette.keys.first()) }

    Column(
        Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        TextButton(onClick=onBack){Text("‹ Volver")}
        Text("Categorías",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(
            "Organiza tus cuentas y asigna un color visual a cada grupo.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Field(
                    if(editing==null)"Nueva categoría" else "Renombrar categoría",
                    text,
                    {text=it}
                )
                Text("Color",fontWeight=FontWeight.SemiBold)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(10.dp)
                ){
                    categoryPalette.forEach { (hex,color)->
                        Box(
                            Modifier.size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedHex=hex },
                            contentAlignment=Alignment.Center
                        ){
                            if(selectedHex==hex){
                                Text("✓",color=Color.White,fontWeight=FontWeight.Black)
                            }
                        }
                    }
                }
                Button(
                    onClick={
                        val name=text.trim()
                        val old=editing
                        if(name.isBlank()||v.categories.any { it.equals(name,true)&&it!=old }){
                            onMessage("Elige un nombre único")
                        }else{
                            val newCategories=if(old==null){
                                v.categories+name
                            }else{
                                v.categories.map { if(it==old)name else it }
                            }
                            val newAccounts=v.accounts.map {
                                if(it.category==old)it.copy(category=name) else it
                            }
                            val newColors=v.categoryColors.toMutableMap().apply {
                                if(old!=null&&old!=name)remove(old)
                                put(name,selectedHex)
                            }
                            onUpdate(v.copy(
                                categories=newCategories,
                                accounts=newAccounts,
                                categoryColors=newColors
                            ))
                            editing=null
                            text=""
                            selectedHex=categoryPalette.keys.first()
                        }
                    },
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text(if(editing==null)"Crear categoría" else "Guardar categoría")
                }
            }
        }

        v.categories.forEach { cat->
            val accent=categoryColor(cat,v.categoryColors)
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
                    Column(Modifier.weight(1f)){
                        Text(cat,fontWeight=FontWeight.Bold)
                        Text(
                            "${v.accounts.count { it.category==cat }} cuentas",
                            style=MaterialTheme.typography.bodySmall
                        )
                    }
                    if(cat!="Sin categoría"){
                        TextButton(onClick={
                            editing=cat
                            text=cat
                            selectedHex=categoryColorHex(cat,v.categoryColors)
                        }){Text("Editar")}
                        TextButton(onClick={
                            val colors=v.categoryColors.toMutableMap().apply { remove(cat) }
                            onUpdate(v.copy(
                                categories=v.categories-cat,
                                accounts=v.accounts.map {
                                    if(it.category==cat)it.copy(category="Sin categoría") else it
                                },
                                categoryColors=colors
                            ))
                            if(editing==cat){
                                editing=null
                                text=""
                                selectedHex=categoryPalette.keys.first()
                            }
                        }){Text("Eliminar",color=MaterialTheme.colorScheme.error)}
                    }
                }
            }
        }
    }
}

@Composable private fun BackupScreen(busy:Boolean,action:(String)->Unit){
    val context=LocalContext.current
    val settings=remember { BackupSettings(context) }
    var autoEnabled by remember { mutableStateOf(settings.driveEnabled) }
    LaunchedEffect(busy){ autoEnabled=settings.driveEnabled }
    val last=if(settings.lastBackupEpochMillis>0) LimaClock.nowLabel(settings.lastBackupEpochMillis) else "Aún no realizado"

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
        Text("Respaldo y recuperación",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(
            "CHETO cifra tus datos antes de guardarlos o subirlos.",
            style=MaterialTheme.typography.bodyMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(24.dp),
            colors=CardDefaults.cardColors(
                containerColor=if(autoEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ){
            Row(
                Modifier.padding(18.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)
            ){
                Icon(
                    if(autoEnabled)Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                    contentDescription=null,
                    modifier=Modifier.size(34.dp),
                    tint=if(autoEnabled)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                    Text(
                        if(autoEnabled)"Backup automático activo" else "Backup automático no configurado",
                        fontWeight=FontWeight.Bold
                    )
                    Text("Última copia: $last",style=MaterialTheme.typography.bodySmall)
                    Text(
                        if(autoEnabled)
                            "Se programa aproximadamente cada 24 horas y tras cambios importantes."
                        else
                            "Conecta Google Drive una vez para habilitarlo.",
                        style=MaterialTheme.typography.bodySmall
                    )
                    settings.lastError?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "Aviso: $it",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Icon(Icons.Rounded.Backup,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                    Text("Archivo .cheto",fontWeight=FontWeight.Bold,fontSize=17.sp)
                }
                Text(
                    "Crea una copia cifrada para guardar en PC, USB o cualquier almacenamiento.",
                    style=MaterialTheme.typography.bodySmall
                )
                Button(onClick={action("export")},enabled=!busy,modifier=Modifier.fillMaxWidth()){
                    Text("Exportar copia cifrada")
                }
                OutlinedButton(onClick={action("restore")},enabled=!busy,modifier=Modifier.fillMaxWidth()){
                    Text("Restaurar desde archivo")
                }
            }
        }

        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Icon(Icons.Rounded.Cloud,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                    Text("Google Drive",fontWeight=FontWeight.Bold,fontSize=17.sp)
                }
                Text(
                    "La copia se cifra antes de subirla a la carpeta privada de CHETO en Drive.",
                    style=MaterialTheme.typography.bodySmall
                )
                Button(onClick={action("drive")},enabled=!busy,modifier=Modifier.fillMaxWidth()){
                    Text(if(autoEnabled)"Sincronizar ahora" else "Conectar Google Drive")
                }
                OutlinedButton(onClick={action("driveRestore")},enabled=!busy,modifier=Modifier.fillMaxWidth()){
                    Text("Restaurar última copia de Drive")
                }
                if(autoEnabled)OutlinedButton(
                    onClick={
                        settings.driveEnabled=false
                        BackupScheduler.disable(context)
                        autoEnabled=false
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text("Desactivar backup automático")
                }
            }
        }

        Text(
            "Guarda tu contraseña de recuperación fuera del teléfono. Sin ella no se puede descifrar una copia.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable private fun ProfileScreen(
    v:MobileVault,
    onUpdate:(MobileVault)->Unit,
    onPhoto:()->Unit,
    onMessage:(String)->Unit
){
    var name by remember(v.name) { mutableStateOf(v.name) }
    var email by remember { mutableStateOf("") }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
        Card(
            Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(26.dp),
            colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)
        ){
            Row(
                Modifier.padding(18.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)
            ){
                Avatar(v.name,v.photo)
                Column(Modifier.weight(1f)){
                    Text(v.name.ifBlank{"Perfil CHETO"},fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Text(
                        v.emails.firstOrNull() ?: "Sin correo principal",
                        style=MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${v.accounts.size} cuentas · ${v.categories.size} categorías",
                        style=MaterialTheme.typography.labelMedium,
                        color=MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick=onPhoto){Text("Foto")}
            }
        }

        Text("Datos del perfil",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Field("Nombre",name,{name=it})
        Button(
            onClick={
                if(name.isBlank())onMessage("Escribe un nombre")
                else{
                    onUpdate(v.copy(name=name.trim()))
                    onMessage("Perfil guardado")
                }
            },
            modifier=Modifier.fillMaxWidth()
        ){Text("Guardar nombre")}

        Text("Correos",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text(
            "Por ahora se guardan como datos locales del perfil. La verificación con Google y Microsoft se agregará mediante OAuth.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant
        )

        v.emails.forEachIndexed { index,e->
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    Avatar(EmailProvider.nameFor(e),EmailProvider.logoUrlFor(e).orEmpty())
                    Column(Modifier.weight(1f)){
                        Text(e,fontWeight=FontWeight.Bold)
                        Text(EmailProvider.nameFor(e),style=MaterialTheme.typography.bodySmall)
                        if(index==0){
                            Surface(
                                shape=RoundedCornerShape(50),
                                color=MaterialTheme.colorScheme.primaryContainer
                            ){
                                Text(
                                    "Principal",
                                    modifier=Modifier.padding(horizontal=8.dp,vertical=3.dp),
                                    style=MaterialTheme.typography.labelSmall,
                                    color=MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row{
                            if(index!=0)TextButton(
                                onClick={onUpdate(v.copy(emails=listOf(e)+(v.emails-e)))}
                            ){Text("Hacer principal")}
                            TextButton(
                                onClick={onUpdate(v.copy(emails=v.emails-e))}
                            ){Text("Eliminar")}
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Icon(Icons.Rounded.Email,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                    Text("Agregar correo",fontWeight=FontWeight.Bold)
                }
                Field("correo@dominio.com",email,{email=it},keyboard=KeyboardType.Email)
                Button(
                    onClick={
                        val e=email.trim()
                        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()||v.emails.any { it.equals(e,true) }){
                            onMessage("Introduce un correo válido y no repetido")
                        }else{
                            onUpdate(v.copy(emails=v.emails+e))
                            email=""
                        }
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text("Agregar al perfil")}
            }
        }
    }
}

@Composable private fun SettingsScreen(
    v:MobileVault,
    onUpdate:(MobileVault)->Unit,
    onCategories:()->Unit,
    onPin:()->Unit
){
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
        Text("Seguridad y preferencias",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)

        Card(
            Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(24.dp),
            colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)
        ){
            Row(
                Modifier.padding(18.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)
            ){
                Icon(Icons.Rounded.Security,contentDescription=null,modifier=Modifier.size(34.dp))
                Column{
                    Text("Bóveda local protegida",fontWeight=FontWeight.Bold)
                    Text(
                        "PIN + Android Keystore + bloqueo al salir",
                        style=MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text("Seguridad",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Setting(
            Icons.Rounded.Fingerprint,
            "Entrar con biometría",
            "Usa huella o biometría fuerte cuando esté disponible.",
            v.biometric
        ){onUpdate(v.copy(biometric=it))}
        Setting(
            Icons.Rounded.VisibilityOff,
            "Ocultar códigos",
            "Los TOTP permanecen ocultos hasta que los reveles.",
            v.hideCodes
        ){onUpdate(v.copy(hideCodes=it))}
        Setting(
            Icons.Rounded.Screenshot,
            "Permitir capturas",
            "Desactivado protege la pantalla con FLAG_SECURE.",
            v.screenshots
        ){onUpdate(v.copy(screenshots=it))}

        Text("Apariencia",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Setting(
            Icons.Rounded.DarkMode,
            "Modo oscuro",
            "Cambia el tema completo de CHETO.",
            v.dark
        ){onUpdate(v.copy(dark=it))}

        Text("Administración",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        OutlinedButton(onClick=onPin,modifier=Modifier.fillMaxWidth()){
            Icon(Icons.Rounded.Key,contentDescription=null)
            Spacer(Modifier.width(8.dp))
            Text("Cambiar PIN")
        }
        OutlinedButton(onClick=onCategories,modifier=Modifier.fillMaxWidth()){
            Icon(Icons.Rounded.Category,contentDescription=null)
            Spacer(Modifier.width(8.dp))
            Text("Gestionar categorías")
        }

        Text(
            "CHETO Authenticator 0.7.0\nAndroid nativo · Kotlin + Jetpack Compose",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable private fun Setting(
    icon:ImageVector,
    label:String,
    description:String,
    value:Boolean,
    onChange:(Boolean)->Unit
){
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){
        Row(
            Modifier.padding(horizontal=14.dp,vertical=12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)
        ){
            Icon(icon,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)){
                Text(label,fontWeight=FontWeight.SemiBold)
                Text(
                    description,
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked=value,onCheckedChange=onChange)
        }
    }
}

@Composable private fun Panel(title:String,description:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,fontWeight=FontWeight.Bold);Text(description,style=MaterialTheme.typography.bodyMedium)}}}
@Composable internal fun Field(label:String,value:String,onChange:(String)->Unit,password:Boolean=false,keyboard:KeyboardType=KeyboardType.Text,singleLine:Boolean=true){
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=singleLine,
        visualTransformation=if(password)PasswordVisualTransformation() else VisualTransformation.None,keyboardOptions=KeyboardOptions(keyboardType=keyboard),shape=ControlShape,
        colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant))
}
@Composable private fun Choice(label:String,value:String,values:List<String>,onChange:(String)->Unit){
    var expanded by remember{mutableStateOf(false)}
    Box{OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().height(48.dp),shape=ControlShape){Text("$label: $value ▾",maxLines=1)}
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){values.forEach { item->DropdownMenuItem(text={Text(item)},onClick={onChange(item);expanded=false}) }}
    }
}
@Composable internal fun Avatar(name:String,photo:String,size:Int=48){
    val bitmap=remember(photo){
        runCatching {
            if(photo.startsWith("data:image/")){
                Base64.decode(photo.substringAfter(','),Base64.DEFAULT)
                    .let { BitmapFactory.decodeByteArray(it,0,it.size)?.asImageBitmap() }
            } else null
        }.getOrNull()
    }
    val remote=remember(name,photo){
        photo.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?: ServiceCatalog.logoUrlFor(name)
    }
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size/3).dp)).background(Blue),
        contentAlignment=Alignment.Center
    ){
        Text(name.take(2).uppercase().ifBlank{"CH"},color=Color.White,fontWeight=FontWeight.Bold)
        when {
            bitmap!=null -> Image(
                bitmap,
                contentDescription="$name logo",
                modifier=Modifier.fillMaxSize(),
                contentScale=ContentScale.Crop
            )
            remote!=null -> AsyncImage(
                model=remote,
                contentDescription="$name logo",
                modifier=Modifier.fillMaxSize().background(Color.White).padding(6.dp),
                contentScale=ContentScale.Fit
            )
        }
    }
}
@Composable private fun CriticalActionDialog(
    title:String,
    description:String,
    biometricAvailable:Boolean,
    onDismiss:()->Unit,
    onVerifyPin:(String)->Boolean,
    onVerifyBiometric:((()->Unit))->Unit,
    onConfirmed:()->Unit,
    onMessage:(String)->Unit
){
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(title)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text(description)
                Field(
                    "PIN de CHETO",
                    pin,
                    {pin=it.filter(Char::isDigit).take(6)},
                    password=true,
                    keyboard=KeyboardType.NumberPassword
                )
                if(biometricAvailable){
                    FilledTonalButton(
                        onClick={onVerifyBiometric(onConfirmed)},
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Icon(Icons.Rounded.Fingerprint,contentDescription=null)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirmar con biometría")
                    }
                }
            }
        },
        confirmButton={
            TextButton(onClick={
                if(pin.length!=6) onMessage("Introduce tu PIN de 6 dígitos")
                else if(onVerifyPin(pin)) onConfirmed()
            }){Text("Confirmar con PIN")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}}
    )
}

@Composable private fun ChangePinDialog(
    onDismiss:()->Unit,
    onConfirm:(String,String)->Unit,
    onMessage:(String)->Unit
){
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Cambiar PIN")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("Confirma tu PIN actual y define uno nuevo de 6 dígitos.")
                Field("PIN actual",current,{current=it.filter(Char::isDigit).take(6)},password=true,keyboard=KeyboardType.NumberPassword)
                Field("Nuevo PIN",next,{next=it.filter(Char::isDigit).take(6)},password=true,keyboard=KeyboardType.NumberPassword)
                Field("Confirmar nuevo PIN",confirm,{confirm=it.filter(Char::isDigit).take(6)},password=true,keyboard=KeyboardType.NumberPassword)
            }
        },
        confirmButton={
            TextButton(onClick={
                when{
                    current.length!=6 -> onMessage("Introduce tu PIN actual de 6 dígitos")
                    next.length!=6 -> onMessage("El nuevo PIN debe tener 6 dígitos")
                    next!=confirm -> onMessage("Los nuevos PIN no coinciden")
                    current==next -> onMessage("El nuevo PIN debe ser diferente")
                    else -> onConfirm(current,next)
                }
            }){Text("Cambiar PIN")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}}
    )
}

@Composable private fun PasswordDialog(title:String,description:String,onDismiss:()->Unit,onConfirm:(String)->Unit,numeric:Boolean=false){
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(description);Field(if(numeric)"Nuevo PIN" else "Contraseña",password,{password=if(numeric)it.filter(Char::isDigit).take(6) else it},password=true,keyboard=if(numeric)KeyboardType.NumberPassword else KeyboardType.Password)
    }},confirmButton={TextButton(onClick={onConfirm(password)}){Text("Continuar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}
