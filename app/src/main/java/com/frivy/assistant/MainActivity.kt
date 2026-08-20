package com.frivy.assistant
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.frivy.assistant.voice.VoiceController

class MainActivity : ComponentActivity() {
    private val vm by viewModels<FrivyViewModel>()
    private var voice: VoiceController? = null
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); voice = VoiceController(this) { vm.send(it) }; setContent { FrivyTheme { FrivyApp(vm, { voice?.listen() }, { startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS")) }) } } }
    fun openQuickSettings() { sendBroadcast(Intent("android.settings.panel.action.INTERNET_CONNECTIVITY")) }
    override fun onDestroy() { voice?.destroy(); super.onDestroy() }
}

@Composable fun FrivyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(background=Color(0xFF090A0F), surface=Color(0xFF14151D), primary=Color(0xFF55C7FF), secondary=Color(0xFF8B5CF6), onSurface=Color(0xFFF4F6FF)), content=content)
}
@Composable fun FrivyApp(vm: FrivyViewModel, onListen: () -> Unit, onAccessibility: () -> Unit) {
    var input by remember { mutableStateOf("") }; var showSettings by remember { mutableStateOf(false) }
    if(showSettings) SettingsScreen({showSettings=false}, onAccessibility) else Scaffold(containerColor=Color(0xFF090A0F)) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top=18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("FRIVY", fontSize=25.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
                TextButton({showSettings=true}) { Text("Settings", color=Color(0xFF55C7FF), fontSize=16.sp) }
            }
            EnergyCore(Modifier.fillMaxWidth().height(220.dp))
            val state by vm.state.collectAsState()
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding=PaddingValues(bottom=12.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                items(state.messages, key={it.id}) { message -> MessageBubble(message) }
                if(state.isThinking) item { ThinkingBubble() }
            }
            state.error?.let { Text(it, color=Color(0xFFFF8A9B), modifier=Modifier.padding(8.dp)) }
            Row(Modifier.fillMaxWidth().padding(bottom=12.dp), verticalAlignment=Alignment.CenterVertically) {
                OutlinedTextField(input,{input=it},Modifier.weight(1f),placeholder={Text("Ask FRIVY anything...",color=Color(0xFF777985))},singleLine=false,shape=RoundedCornerShape(10.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Color(0xFF8B5CF6), unfocusedBorderColor=Color(0xFF8B5CF6), cursorColor=Color(0xFF55C7FF)))
                Spacer(Modifier.width(9.dp)); IconButton(onClick={ if(input.isNotBlank()){vm.send(input);input=""} else onListen()},Modifier.size(58.dp).background(Color(0xFF7650C7),CircleShape)) { Icon(if(input.isBlank()) Icons.Default.Mic else Icons.Default.Send,"Send",tint=Color.White) }
            }
        }
    }
}
@Composable fun EnergyCore(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label="core"); val pulse by infinite.animateFloat(0.88f,1.12f,infiniteRepeatable(tween(1500,Easing.InOutSine),RepeatMode.Reverse),label="pulse")
    Box(modifier,Alignment.Center) { Box(Modifier.size((180*pulse).dp).blur(38.dp).background(Color(0xFF0D8FCA).copy(.40f),CircleShape)); Box(Modifier.size(72.dp).scale(pulse).background(Color(0xFF55C7FF),CircleShape)); repeat(3) { i -> Box(Modifier.offset(x=(if(i==0) (-35).dp else if(i==1) 64.dp else (-35).dp), y=(if(i==0)(-54).dp else if(i==1)(-8).dp else 55.dp)).size(7.dp).background(Color(0xFF55C7FF),CircleShape)) } }
}
@Composable fun MessageBubble(message: com.frivy.assistant.data.Message) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement=if(message.role=="user") Arrangement.End else Arrangement.Start) {
        Surface(color=if(message.role=="user") Color(0xFF55C7FF) else Color(0xFF1B1C24), shape=RoundedCornerShape(15.dp), modifier=Modifier.widthIn(max=350.dp)) { Text(message.content,Modifier.padding(14.dp),color=if(message.role=="user") Color(0xFF061019) else Color(0xFFF4F6FF),fontSize=17.sp,lineHeight=24.sp) }
    }
}
@Composable fun ThinkingBubble() { Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) { repeat(3) { i -> val a=rememberInfiniteTransition(label="dot").animateFloat(.35f,1f,infiniteRepeatable(tween(500,delayMillis=i*120),RepeatMode.Reverse),label="alpha"); Box(Modifier.size(7.dp).alpha(a.value).background(Color(0xFF55C7FF),CircleShape)) } } }
@Composable fun SettingsScreen(onBack:()->Unit,onAccessibility:()->Unit) { var voice by remember{mutableStateOf(true)}; var wake by remember{mutableStateOf(true)}; Column(Modifier.fillMaxSize().background(Color(0xFF090A0F)).padding(20.dp)) { Row(verticalAlignment=Alignment.CenterVertically){IconButton(onBack){Icon(Icons.Default.ArrowBack,"Back")}; Text("Settings",fontSize=25.sp,fontWeight=FontWeight.Bold)}; Spacer(Modifier.height(25.dp)); SettingToggle("Voice mode","FRIVY speaks responses",voice){voice=it}; SettingToggle("Wake word","Listen for Hey FRIVY",wake){wake=it}; SettingToggle("Accessibility mode","Allow screen reading and automation",false){if(it)onAccessibility()}; Text("AI model",Modifier.padding(top=22.dp,bottom=8.dp),color=Color(0xFF8D8E99)); Text("openai/gpt-oss-120b",Modifier.fillMaxWidth().background(Color(0xFF15161E),RoundedCornerShape(12.dp)).padding(16.dp),color=Color(0xFF55C7FF)) } }
@Composable fun SettingToggle(title:String,subtitle:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=13.dp),Arrangement.SpaceBetween,Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontSize=17.sp);Text(subtitle,color=Color(0xFF8D8E99),fontSize=13.sp)};Switch(value,onChange,colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Color(0xFF55C7FF)))}}