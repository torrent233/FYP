package com.example.greetingcard
import androidx.compose.ui.res.painterResource
import org.json.JSONObject
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greetingcard.ui.theme.GreetingcardTheme
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.util.Log
/*
// 创建 ViewModel 来管理图片的状态
class ImageViewModel : ViewModel() {
    // 模拟接收到的图片列表（可以替换为实际的图片资源）
    var images = listOf(R.drawable.image1, R.drawable.image2, R.drawable.image3)
        private set

    // 存储当前显示的图片索引
    var currentIndex by mutableStateOf(0)

    // 获取当前显示的图片资源
    val currentImage: Int
        get() = images[currentIndex]

    // 切换到下一张图片
    fun nextImage() {
        currentIndex = (currentIndex + 1) % images.size  // 循环切换图片
    }
}

class MainActivity : ComponentActivity() {
    // 使用 ViewModel 来管理图片状态
    private val imageViewModel: ImageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreetingcardTheme {
                // Scaffold 用于布局结构，innerPadding 可以确保 UI 内容不被遮挡
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 传递 imageViewModel 到 MainScreen 以进行图片展示和切换
                    MainScreen(modifier = Modifier.padding(innerPadding), imageViewModel = imageViewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, imageViewModel: ImageViewModel) {
    Column(modifier = modifier.padding(16.dp)) {
        // 顶部区域：展示个人信息和设置按钮
        ProfileSection()

        Spacer(modifier = Modifier.height(20.dp))

        // 显示当前图片
        DisplayImage(imageRes = imageViewModel.currentImage)

        Spacer(modifier = Modifier.height(20.dp))

        // 切换图片的按钮，点击后调用 ViewModel 的 nextImage 方法切换到下一张图片
        Button(onClick = { imageViewModel.nextImage() }) {
            Text("Next Image")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 显示功能区域的卡片（例如：Screen Monitoring）
        CardSection(title = "Screen Monitoring", icon = R.drawable.ic_screen_monitoring)
        CardSection(title = "Screen Locked", icon = R.drawable.ic_screen_locked)
        CardSection(title = "Records Review", icon = R.drawable.ic_records_review)
    }
}

// 显示当前图片的 Composable 函数
@Composable
fun DisplayImage(imageRes: Int) {
    // 使用 Image 组件显示当前的图片
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Received Image",
        modifier = Modifier.fillMaxWidth().height(300.dp).padding(8.dp)  // 设置图片的宽度和高度
    )
}

@Composable
fun ProfileSection() {
    // 顶部区域，包含头像和名字，设置按钮
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 显示头像
        Image(
            painter = painterResource(id = R.drawable.ic_avatar),
            contentDescription = "Profile Image",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 显示名字
        Text(text = "My Child", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))  // 自适应空白以将设置按钮推到右侧
        // 设置按钮
        IconButton(onClick = { /* Handle Settings Click */ }) {
            Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Settings")
        }
    }
}

@Composable
fun TotalScreenTime() {
    // 显示总屏幕时间和提醒信息
    Column {
        Text(text = "Total Screen Time", color = Color.Gray)
        Text(text = "1d 13h 22min", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Remind children to rest", color = Color.Green)
    }
}

@Composable
fun CardSection(title: String, icon: Int) {
    // 卡片组件，点击后切换到下一张图片或处理其他操作
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .clickable { /* Handle Card Click */ },
        shape = MaterialTheme.shapes.medium.copy(CornerSize(12.dp)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 显示卡片图标
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // 显示卡片标题
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    GreetingcardTheme {
        // 创建一个新的 ImageViewModel 实例用于预览
        val imageViewModel = ImageViewModel()
        // 显示 MainScreen
        MainScreen(modifier = Modifier, imageViewModel = imageViewModel)
    }
}
*/
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString

// 修改后的 ImageViewModel，同时保留本地图片切换功能，并新增 WebSocket 接收功能
class ImageViewModel : ViewModel() {
    // 模拟接收到的图片列表（可以替换为实际的图片资源）
    var images = listOf(R.drawable.image1, R.drawable.image2, R.drawable.image3)
        private set

    // 存储当前显示的图片索引（用于模拟图片切换）
    var currentIndex by mutableStateOf(0)

    // 获取当前显示的图片资源（本地模拟）
    val currentImage: Int
        get() = images[currentIndex]

    // 切换到下一张图片（模拟功能）
    fun nextImage() {
        currentIndex = (currentIndex + 1) % images.size  // 循环切换图片
    }
    fun fetchLatestImage() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.43.54:5000/latest_image") // Flask 服务器的截图接口
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Parent", "❌ 拉取截图失败: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.byteStream()?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    receivedBitmap = bitmap
                }
            }
        })
    }

    // 新增：存储从服务器接收到的 Bitmap（截图数据）
    var receivedBitmap by mutableStateOf<Bitmap?>(null)
        private set

    // 新增：通过 WebSocket 连接到服务器接收数据
    fun connectToWebSocket(serverUrl: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(serverUrl).build()
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 可选：发送初始化消息或日志
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // 将接收到的二进制数据转换为 Bitmap，并更新状态
                val byteArray = bytes.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                receivedBitmap = bitmap
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // 错误处理，可在此记录日志或提示
            }
        })
    }
}

class MainActivity : ComponentActivity() {
    // 使用 ViewModel 来管理图片状态和网络数据
    private val imageViewModel: ImageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 这里暂时不自动连接，改为手动点击按钮连接
        setContent {
            GreetingcardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding), imageViewModel = imageViewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, imageViewModel: ImageViewModel) {
    Column(modifier = modifier.padding(16.dp)) {
        // 顶部区域：展示个人信息和设置按钮
        ProfileSection()

        Spacer(modifier = Modifier.height(20.dp))

        // 如果接收到网络图片，则显示网络图片；否则显示本地模拟图片
        val bitmap = imageViewModel.receivedBitmap
        if (bitmap != null) {
            DisplayReceivedImage(bitmap = bitmap)
        } else {
            DisplayImage(imageRes = imageViewModel.currentImage)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 按钮用于切换本地图片（模拟功能）
        Button(onClick = { imageViewModel.nextImage() }) {
            Text("Next Image")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 新增按钮：连接到服务器，接收孩子端数据
        Button(onClick = { imageViewModel.connectToWebSocket("http://192.168.43.54:5000") }) {
            Text("Connect to Server")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 功能区域的卡片（示例）
        CardSection(title = "Screen Monitoring", icon = R.drawable.ic_screen_monitoring,onClick = { imageViewModel.fetchLatestImage() })
        CardSection(
            title = "Screen Locked",
            icon = R.drawable.ic_screen_locked,
            onClick = {
                sendLockRequest() // 🧨 父母点击“锁屏”后调用此函数
            }
        )

        CardSection(title = "Records Review", icon = R.drawable.ic_records_review)
    }
}

@Composable
fun DisplayImage(imageRes: Int) {
    // 使用 Image 组件显示本地资源图片
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Received Image",
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp)
    )
}

@Composable
fun DisplayReceivedImage(bitmap: Bitmap) {
    // 使用 Image 组件显示从服务器接收到的 Bitmap 图片
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Received Image from Server",
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp)
    )
}

@Composable
fun ProfileSection() {
    // 顶部区域，包含头像、名字和设置按钮
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.ic_avatar),
            contentDescription = "Profile Image",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "My Child", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { /* Handle Settings Click */ }) {
            Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Settings")
        }
    }
}

@Composable
fun CardSection(title: String, icon: Int, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }, // 点击触发传入的逻辑
        shape = MaterialTheme.shapes.medium.copy(CornerSize(12.dp)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

fun sendLockRequest() {
    val client = OkHttpClient()
    val json = JSONObject().put("label", "parent")

    val requestBody = json.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url("http://192.168.43.54:5000/lock")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("Parent", "❌ Lock request failed: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            Log.d("Parent", "✅ Lock command sent successfully")
        }
    })
}


@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    GreetingcardTheme {
        // 预览时，使用一个新的 ImageViewModel 实例
        val imageViewModel = ImageViewModel()
        MainScreen(modifier = Modifier, imageViewModel = imageViewModel)
    }
}
