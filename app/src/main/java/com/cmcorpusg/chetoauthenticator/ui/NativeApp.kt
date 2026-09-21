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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmcorpusg.chetoauthenticator.core.TotpEngine
import com.cmcorpusg.chetoauthenticator.data.MobileAccount
import com.cmcorpusg.chetoauthenticator.data.MobileVault
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private val Blue=Color(0xFF3157F6)
private val Purple=Color(0xFF7144E8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeApp(
    vault:MobileVault?,exists:Boolean,busy:Boolean,scanned:MobileAccount?,stagedPhoto:String?,
    onLogin:(String)->Unit,onRegister:(String,String,String)->Unit,onBiometric:()->Unit,
    onUpdate:(MobileVault)->Unit,onScan:(Boolean)->Unit,onScannedConsumed:()->Unit,onCopy:(String)->Unit,
    onBackup:(String,String)->Unit,onPhoto:(String?)->Unit,onPhotoConsumed:()->Unit,onLock:()->Unit,onMessage:(String)->Unit
){
    MaterialTheme(colorScheme=if(vault?.dark==true)darkColorScheme(primary=Color(0xFF98AAFF)) else lightColorScheme(primary=Blue,background=Color(0xFFF4F6FB))){
        Surface(Modifier.fillMaxSize()){
            if(vault==null){LoginScreen(exists,onLogin,onRegister,onBiometric,onMessage);return@Surface}
            var page by remember { mutableStateOf("Inicio") }
            var editor by remember { mutableStateOf<MobileAccount?>(null) }
            var delete by remember { mutableStateOf<MobileAccount?>(null) }
            var backupMode by remember { mutableStateOf<String?>(null) }
            var changePin by remember { mutableStateOf(false) }
            var manageCategories by remember { mutableStateOf(false) }
            var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit){ while(true){ clockMillis=System.currentTimeMillis(); delay(30_000) } }
            LaunchedEffect(scanned){if(scanned!=null){editor=scanned;onScannedConsumed()}}
            LaunchedEffect(stagedPhoto){if(stagedPhoto!=null&&editor!=null){editor=editor!!.copy(photo=stagedPhoto);onPhotoConsumed()}}
            BackHandler { when { editor!=null->editor=null;manageCategories->manageCategories=false;page!="Inicio"->page="Inicio";else->onLock() } }
            Scaffold(
                topBar={
                    Column(Modifier.background(Brush.linearGradient(listOf(Blue,Purple))).statusBarsPadding().fillMaxWidth().padding(20.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){
                            Text("CHETO",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black)
                            Text(if(manageCategories)"Categorías" else page,color=Color.White.copy(alpha=.8f),fontSize=13.sp)
                            Text("Lima · " + LimaClock.nowLabel(clockMillis),color=Color.White.copy(alpha=.72f),fontSize=11.sp)
                        };TextButton(onClick=onLock){
                            Icon(Icons.Rounded.Lock,contentDescription="Bloquear",tint=Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Bloquear",color=Color.White)
                        }}
                    }
                },bottomBar={if(!manageCategories)NavigationBar {
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
                }},floatingActionButton={if(page=="Inicio"&&!manageCategories)FloatingActionButton(onClick={editor=MobileAccount()},containerColor=Blue,contentColor=Color.White){Icon(Icons.Rounded.Add,contentDescription="Agregar cuenta")}}
            ){padding->
                Column(Modifier.padding(padding).fillMaxSize()){
                    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(manageCategories){Categories(vault,onUpdate,{manageCategories=false},onMessage)}
                    else when(page){
                        "Inicio"->Accounts(vault,onCopy,{editor=it},{delete=it},onScan,{manageCategories=true})
                        "Backup"->BackupScreen(busy){backupMode=it}
                        "Perfil"->ProfileScreen(vault,onUpdate,{onPhoto(null)},onMessage)
                        "Ajustes"->SettingsScreen(vault,onUpdate,{manageCategories=true},{changePin=true})
                    }
                }
            }
            editor?.let { account->AccountEditor(account,vault.categories,onPhoto={onPhoto(account.id)},onDismiss={editor=null},onSave={a->
                onUpdate(vault.copy(accounts=if(vault.accounts.any { it.id==a.id })vault.accounts.map { if(it.id==a.id)a else it } else vault.accounts+a));editor=null
            },onMessage=onMessage) }
            delete?.let { account->AlertDialog(onDismissRequest={delete=null},title={Text("Eliminar cuenta")},text={Text("¿Eliminar ${account.issuer} (${account.label})? Conserva una copia antes de eliminarla.")},confirmButton={TextButton(onClick={onUpdate(vault.copy(accounts=vault.accounts.filterNot { it.id==account.id }));delete=null}){Text("Eliminar")}},dismissButton={TextButton(onClick={delete=null}){Text("Cancelar")}}) }
            backupMode?.let { mode->PasswordDialog(
                title=if(mode.contains("estore"))"Restaurar copia" else "Crear copia cifrada",
                description=if(mode.contains("estore"))"Reemplazará las cuentas y el perfil actuales. Introduce la contraseña de la copia." else "Usa al menos 10 caracteres. Guarda esta contraseña: la necesitarás para recuperar tus cuentas.",
                onDismiss={backupMode=null},onConfirm={p->if(p.isBlank()||(!mode.contains("estore")&&p.length<10))onMessage("Revisa la contraseña") else {backupMode=null;onBackup(mode,p)}}) }
            if(changePin)PasswordDialog("Cambiar PIN","Introduce un nuevo PIN de 6 dígitos.",{changePin=false},{p->
                if(p.matches(Regex("[0-9]{6}"))){onUpdate(vault.copy(pin=p));changePin=false;onMessage("PIN actualizado")}else onMessage("Debe tener 6 dígitos")
            },numeric=true)
        }
    }
}

@Composable private fun LoginScreen(exists:Boolean,onLogin:(String)->Unit,onRegister:(String,String,String)->Unit,onBio:()->Unit,onMessage:(String)->Unit){
    var name by remember { mutableStateOf("") };var email by remember { mutableStateOf("") };var pin by remember { mutableStateOf("") };var confirm by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Blue,Purple))).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Text("◈",color=Color.White,fontSize=64.sp);Text("CHETO",color=Color.White,fontSize=36.sp,fontWeight=FontWeight.Black)
        Text("Authenticator · acceso local seguro",color=Color.White.copy(alpha=.8f));Spacer(Modifier.height(28.dp))
        Card(shape=RoundedCornerShape(26.dp)) { Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(exists)"Bienvenido de nuevo" else "Crear perfil local",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            if(!exists){Field("Nombre",name,{name=it});Field("Correo principal",email,{email=it},keyboard=KeyboardType.Email)}
            Field("PIN de 6 dígitos",pin,{pin=it.filter(Char::isDigit).take(6)},password=true,keyboard=KeyboardType.NumberPassword)
            if(!exists)Field("Repite tu PIN",confirm,{confirm=it.filter(Char::isDigit).take(6)},password=true,keyboard=KeyboardType.NumberPassword)
            Button(onClick={
                if(pin.length!=6)onMessage("El PIN debe tener 6 dígitos")
                else if(exists){onLogin(pin);pin=""}
                else if(name.isBlank()||!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())onMessage("Completa tu nombre y un correo válido")
                else if(pin!=confirm)onMessage("Los PIN no coinciden")
                else onRegister(name.trim(),email.trim(),pin)
            },modifier=Modifier.fillMaxWidth()){Text(if(exists)"Desbloquear" else "Crear perfil")}
            if(exists)OutlinedButton(onClick=onBio,modifier=Modifier.fillMaxWidth()){Text("Entrar con huella")}
            Text("Tus códigos se generan sin internet.",style=MaterialTheme.typography.bodySmall)
        } }
    }
}

@Composable private fun Accounts(v:MobileVault,onCopy:(String)->Unit,onEdit:(MobileAccount)->Unit,onDelete:(MobileAccount)->Unit,onScan:(Boolean)->Unit,onCategories:()->Unit){
    var search by remember { mutableStateOf("") };var category by remember { mutableStateOf("Todos") };var now by remember { mutableLongStateOf(System.currentTimeMillis()/1000) }
    var revealed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit){while(true){now=System.currentTimeMillis()/1000;delay(1000)}}
    LaunchedEffect(revealed){if(revealed!=null){delay(10000);revealed=null}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,96.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(16.dp)){Text("Protege tus cuentas",fontWeight=FontWeight.Bold);Text("Guarda una copia cifrada desde Backup.",style=MaterialTheme.typography.bodySmall)}}}
        item{Field("Buscar cuentas",search,{search=it})}
        item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            (listOf("Todos")+v.categories).forEach { c->FilterChip(selected=c==category,onClick={category=c},label={Text(c)}) }
            AssistChip(onClick=onCategories,label={Text("+ Categorías")})
        }}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={onScan(false)},modifier=Modifier.weight(1f)){Text("Escanear QR")};OutlinedButton(onClick={onScan(true)},modifier=Modifier.weight(1f)){Text("QR de imagen")}}}
        val accounts=v.accounts.filter { (category=="Todos"||it.category==category)&&(it.issuer+" "+it.label).contains(search,true) }
        if(accounts.isEmpty())item{Panel("No hay cuentas","Toca + para agregar una clave o escanea el QR del servicio.")}
        items(accounts,key={it.id}){a->
            val code=remember(a,now/a.period){runCatching { TotpEngine.generate(a.secret,now,a.digits,a.period,a.algorithm) }.getOrDefault("------")}
            val seconds=TotpEngine.secondsRemaining(now,a.period)
            Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
                Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        Avatar(a.issuer,a.photo);Column(Modifier.weight(1f)){Text(a.issuer,fontWeight=FontWeight.Bold);Text(a.label,style=MaterialTheme.typography.bodySmall);Text(a.category,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(if(v.hideCodes&&revealed!=a.id)"••• •••" else code.chunked(if(a.digits==6)3 else 4).joinToString(" "),fontSize=30.sp,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f).clickable { if(v.hideCodes)revealed=a.id else onCopy(code) })
                        Box(contentAlignment=Alignment.Center){CircularProgressIndicator(progress={seconds.toFloat()/a.period},modifier=Modifier.size(42.dp),strokeWidth=4.dp);Text("${seconds}s",fontSize=11.sp)}
                    }
                    Row{TextButton(onClick={onCopy(code)}){Text("Copiar")};TextButton(onClick={onEdit(a)}){Text("Editar")};TextButton(onClick={onDelete(a)}){Text("Eliminar",color=MaterialTheme.colorScheme.error)}}
                }
            }
        }
    }
}

@Composable private fun AccountEditor(initial:MobileAccount,categories:List<String>,onPhoto:()->Unit,onDismiss:()->Unit,onSave:(MobileAccount)->Unit,onMessage:(String)->Unit){
    var a by remember(initial.id) { mutableStateOf(initial) };var period by remember(initial.id){mutableStateOf(initial.period.toString())}
    LaunchedEffect(initial.photo){a=a.copy(photo=initial.photo)}
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
            Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("Cuenta TOTP",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                Row(verticalAlignment=Alignment.CenterVertically){Avatar(a.issuer,a.photo);TextButton(onClick=onPhoto){Text("Elegir logo local")}}
                Field("Servicio",a.issuer,{a=a.copy(issuer=it)});Field("Cuenta o correo",a.label,{a=a.copy(label=it)})
                Field("Clave secreta Base32",a.secret,{a=a.copy(secret=it)},password=true)
                Choice("Categoría",a.category,categories){a=a.copy(category=it)}
                Field("Notas",a.notes,{a=a.copy(notes=it)},singleLine=false)
                Choice("Dígitos",a.digits.toString(),listOf("6","7","8")){a=a.copy(digits=it.toInt())}
                Field("Periodo en segundos",period,{period=it.filter(Char::isDigit).take(3)},keyboard=KeyboardType.Number)
                Choice("Algoritmo",a.algorithm,listOf("SHA1","SHA256","SHA512")){a=a.copy(algorithm=it)}
                Button(onClick={
                    val normalized=a.secret.uppercase().replace(Regex("[\\s=-]"),"")
                    val p=period.toIntOrNull()
                    if(p==null||p !in 1..300||a.issuer.isBlank())onMessage("Completa el servicio y un periodo entre 1 y 300")
                    else runCatching { TotpEngine.generate(normalized,digits=a.digits,period=p,algorithm=a.algorithm) }.onSuccess {
                        val issuer=a.issuer.trim()
                        onSave(a.copy(
                            secret=normalized,
                            period=p,
                            issuer=issuer,
                            label=a.label.trim().ifBlank{"Sin etiqueta"},
                            photo=a.photo.ifBlank { ServiceCatalog.logoUrlFor(issuer).orEmpty() }
                        ))
                    }.onFailure { onMessage("Clave Base32 inválida. Copia la clave del servicio.") }
                },modifier=Modifier.fillMaxWidth()){Text("Guardar cuenta")}
                OutlinedButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()){Text("Cancelar")}
            }
        }
    }
}

@Composable private fun Categories(v:MobileVault,onUpdate:(MobileVault)->Unit,onBack:()->Unit,onMessage:(String)->Unit){
    var text by remember { mutableStateOf("") };var editing by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        TextButton(onClick=onBack){Text("‹ Volver")};Text("Organiza tus cuentas",style=MaterialTheme.typography.titleLarge)
        Field(if(editing==null)"Nueva categoría" else "Renombrar categoría",text,{text=it})
        Button(onClick={val name=text.trim();val old=editing
            if(name.isBlank()||v.categories.any { it.equals(name,true)&&it!=old })onMessage("Elige un nombre único")
            else{onUpdate(v.copy(categories=if(old==null)v.categories+name else v.categories.map { if(it==old)name else it },accounts=v.accounts.map { if(it.category==old)it.copy(category=name) else it }));editing=null;text=""}
        }){Text(if(editing==null)"Crear categoría" else "Guardar nombre")}
        v.categories.forEach { c->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){
            Text("$c · ${v.accounts.count { it.category==c }}",fontWeight=FontWeight.Bold)
            if(c!="Sin categoría")Row{TextButton(onClick={editing=c;text=c}){Text("Editar")};TextButton(onClick={onUpdate(v.copy(categories=v.categories-c,accounts=v.accounts.map { if(it.category==c)it.copy(category="Sin categoría") else it }));if(editing==c){editing=null;text=""}}){Text("Eliminar")}}
        }}}
    }
}

@Composable private fun BackupScreen(busy:Boolean,action:(String)->Unit){
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Panel("Copia de seguridad","Tus cuentas, categorías y perfil viajan cifrados con una contraseña de recuperación.")
        Button(onClick={action("export")},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Exportar archivo .cheto")}
        OutlinedButton(onClick={action("restore")},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Restaurar desde archivo")}
        HorizontalDivider();Text("Google Drive",style=MaterialTheme.typography.titleLarge)
        Text("Copia manual cifrada en la carpeta privada de la app. Requiere configurar OAuth Android para este APK.",style=MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick={action("drive")},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Guardar en Google Drive")}
        OutlinedButton(onClick={action("driveRestore")},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Restaurar desde Google Drive")}
        Text("Conserva tu contraseña fuera del teléfono. Sin ella no se puede descifrar el respaldo.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun ProfileScreen(v:MobileVault,onUpdate:(MobileVault)->Unit,onPhoto:()->Unit,onMessage:(String)->Unit){
    var name by remember(v.name) { mutableStateOf(v.name) };var email by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Avatar(v.name,v.photo);TextButton(onClick=onPhoto){Text("Cambiar foto")}}
        Field("Nombre",name,{name=it});Button(onClick={if(name.isNotBlank()){onUpdate(v.copy(name=name.trim()));onMessage("Perfil guardado")}}){Text("Guardar nombre")}
        Text("Correos locales",style=MaterialTheme.typography.titleLarge)
        Text("Son etiquetas de tu perfil; no inician sesión ni verifican la dirección.",style=MaterialTheme.typography.bodySmall)
        v.emails.forEachIndexed { index,e->Card(Modifier.fillMaxWidth()){
            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Avatar(EmailProvider.nameFor(e),EmailProvider.logoUrlFor(e).orEmpty())
                Column(Modifier.weight(1f)){
                    Text(e,fontWeight=FontWeight.Bold)
                    Text(EmailProvider.nameFor(e),style=MaterialTheme.typography.bodySmall)
                    if(index==0)Text("Principal",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)
                    Row{
                        if(index!=0)TextButton(onClick={onUpdate(v.copy(emails=listOf(e)+(v.emails-e)))}){Text("Hacer principal")}
                        TextButton(onClick={onUpdate(v.copy(emails=v.emails-e))}){Text("Eliminar")}
                    }
                }
            }
        }}
        Field("Agregar correo",email,{email=it},keyboard=KeyboardType.Email)
        Button(onClick={val e=email.trim();if(!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()||v.emails.any { it.equals(e,true) })onMessage("Introduce un correo válido y no repetido")else{onUpdate(v.copy(emails=v.emails+e));email=""}}){Text("Agregar correo")}
    }
}

@Composable private fun SettingsScreen(v:MobileVault,onUpdate:(MobileVault)->Unit,onCategories:()->Unit,onPin:()->Unit){
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Panel("Seguridad local","La app se bloquea al salir. Tus datos se cifran con Android Keystore.")
        Setting("Entrar con biometría",v.biometric){onUpdate(v.copy(biometric=it))}
        Setting("Ocultar códigos",v.hideCodes){onUpdate(v.copy(hideCodes=it))}
        Setting("Permitir capturas de pantalla",v.screenshots){onUpdate(v.copy(screenshots=it))}
        Setting("Modo oscuro",v.dark){onUpdate(v.copy(dark=it))}
        OutlinedButton(onClick=onPin,modifier=Modifier.fillMaxWidth()){Text("Cambiar PIN")}
        OutlinedButton(onClick=onCategories,modifier=Modifier.fillMaxWidth()){Text("Gestionar categorías")}
        Text("CHETO Authenticator 0.5.0\nAndroid nativo · Kotlin + Jetpack Compose",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun Setting(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(checked=value,onCheckedChange=onChange)}}
@Composable private fun Panel(title:String,description:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,fontWeight=FontWeight.Bold);Text(description,style=MaterialTheme.typography.bodyMedium)}}}
@Composable private fun Field(label:String,value:String,onChange:(String)->Unit,password:Boolean=false,keyboard:KeyboardType=KeyboardType.Text,singleLine:Boolean=true){
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=singleLine,
        visualTransformation=if(password)PasswordVisualTransformation() else VisualTransformation.None,keyboardOptions=KeyboardOptions(keyboardType=keyboard),shape=RoundedCornerShape(15.dp))
}
@Composable private fun Choice(label:String,value:String,values:List<String>,onChange:(String)->Unit){
    var expanded by remember{mutableStateOf(false)}
    Box{OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text("$label: $value ▾")}
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){values.forEach { item->DropdownMenuItem(text={Text(item)},onClick={onChange(item);expanded=false}) }}
    }
}
@Composable private fun Avatar(name:String,photo:String){
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
        Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Blue),
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
@Composable private fun PasswordDialog(title:String,description:String,onDismiss:()->Unit,onConfirm:(String)->Unit,numeric:Boolean=false){
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(description);Field(if(numeric)"Nuevo PIN" else "Contraseña",password,{password=if(numeric)it.filter(Char::isDigit).take(6) else it},password=true,keyboard=if(numeric)KeyboardType.NumberPassword else KeyboardType.Password)
    }},confirmButton={TextButton(onClick={onConfirm(password)}){Text("Continuar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}
