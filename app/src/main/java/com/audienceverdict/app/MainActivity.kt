package com.audienceverdict.app


import android.os.Bundle
import android.graphics.Bitmap
import android.os.SystemClock
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import retrofit2.HttpException
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.audienceverdict.app.core.TokenStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.viewinterop.AndroidView

private val Ink = Color(0xFF080A0B)
private val Panel = Color(0xFF15191C)
private val Panel2 = Color(0xFF202529)
private val Red = Color(0xFFFF343B)
private val Muted = Color(0xFFA5A9AF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PendingAvNotification.intent.value=intent?.takeIf{it.hasExtra("av_notification_destination")}
        AvClient.initialize(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
        setContent { AVTheme { AVApp() } }
    }

    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PendingAvNotification.intent.value=intent.takeIf{it.hasExtra("av_notification_destination")}
    }
}

private object PendingAvNotification { val intent=mutableStateOf<Intent?>(null) }

@Composable private fun AVTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary=Red, background=Ink, surface=Panel, onBackground=Color.White, onSurface=Color.White), content=content)
}

data class PageMovies(val content: List<Movie> = emptyList(), val totalPages: Int = 0)
data class PageTheatres(val content:List<Theatre> = emptyList())
data class Movie(val id:String="", val title:String="", val posterUrl:String?=null, val posterImages:List<String>?=null, val backdropUrl:String?=null, val description:String?=null, val synopsis:String?=null, val status:String?=null, val genre:List<String>?=null, val genres:List<String>?=null, val language:String?=null, val originalLanguage:String?=null, val languages:List<String>?=null, val duration:Int?=null, val durationMinutes:Int?=null, val releaseDate:String?=null, val announcementDate:String?=null, val certification:String?=null, val director:String?=null, val cast:List<String>?=null, val production:String?=null, val productionCompanies:List<String>?=null, val officialWebsite:String?=null, val trailerUrl:String?=null, val externalTrailerUrl:String?=null, val media:List<MovieMedia>?=null, val castCredits:List<MovieCredit>?=null, val crewByDepartment:Map<String,List<MovieCredit>>?=null, val tagline:String?=null, val releaseStatus:String?=null)
data class MovieMedia(val id:String="",val mediaType:String="",val title:String?=null,val description:String?=null,val mediaUrl:String?=null,val thumbnailUrl:String?=null,val embedUrl:String?=null,val language:String?=null,val isOfficial:Boolean=false,val isPrimary:Boolean=false)
data class PersonSocialLink(val platform:String?=null,val profileUrl:String?=null,val username:String?=null)
data class MoviePerson(val fullName:String?=null,val profileImageUrl:String?=null,val biography:String?=null,val officialWebsite:String?=null,val socialLinks:List<PersonSocialLink>?=null)
data class MovieCredit(val creditType:String?=null,val department:String?=null,val roleTitle:String?=null,val characterName:String?=null,val characterImageUrl:String?=null,val characterCategory:String?=null,val isMainCast:Boolean=false,val person:MoviePerson?=null)
data class OtpPayload(val destination:String)
data class VerifyPayload(val destination:String, val otp:String)
data class ApiMessage(val message:String?=null, val success:Boolean?=null, val resendAfterSeconds:Long?=null)
data class OtpResult(val accessToken:String?=null, val registrationRequired:Boolean=false)
data class RegistrationOtpResult(val accessToken:String?=null,val registrationRequired:Boolean=false,val registrationToken:String?=null)
data class RegistrationEmailRequest(val registrationToken:String,val email:String)
data class RegistrationEmailVerifyRequest(val registrationToken:String,val email:String,val otp:String)
data class RegistrationCompletePayload(val registrationToken:String,val name:String,val email:String,val mobile:String,val emailVerificationToken:String)
data class Me(val id:String?=null,val name:String?=null,val email:String?=null,@SerializedName(value="mobile",alternate=["mobileNumber","phoneNumber"]) val mobile:String?=null,@SerializedName(value="alternativeMobile",alternate=["alternateMobile"]) val alternativeMobile:String?=null,@SerializedName(value="mobileVerified",alternate=["isMobileVerified","phoneVerified"]) val mobileVerified:Boolean?=null,@SerializedName(value="alternativeMobileVerified",alternate=["isAlternativeMobileVerified","alternateMobileVerified"]) val alternativeMobileVerified:Boolean?=null)
data class UpdateProfilePayload(val name:String,val email:String,val alternativeMobile:String?=null)
data class AvNotification(val id:String?=null,val title:String?=null,val message:String?=null,val body:String?=null,val type:String?=null,val createdAt:String?=null,val read:Boolean?=null,val movieId:String?=null,val movieTitle:String?=null,val bookingId:String?=null,val targetId:String?=null)
data class WaitingListRequest(val showId:String,val requestedSeatsCount:Int=1)
data class WaitingListEntry(val id:String="",val showId:String="",val userId:String?=null,val requestedSeatsCount:Int=1,val position:Int?=null,val status:String="WAITING",val offerExpiresAt:String?=null,val createdAt:String?=null,val updatedAt:String?=null)
data class Show(val id:String="",val movieId:String="",val theatreId:String="",val date:String="",val startTime:String="",val ticketType:String="",val ticketPrice:Double=0.0,val status:String="",val seatPrices:Map<String,Double>?=null,val maxTicketsPerMobile:Int?=null,val holdMinutes:Int?=null)
data class Theatre(val id:String="",val name:String="",val address:String?=null,val city:String?=null)
data class Review(val id:String="",val userId:String?=null,val author:String?=null,val title:String?=null,val text:String?=null,val rating:Int?=null,val status:String?=null,val thumbnail:String?=null,val createdAt:String?=null)
data class BookingRecord(val id:String="",val showId:String="",val seatIds:List<String> = emptyList(),val ticketCount:Int=0,val totalAmount:Double=0.0,val status:String="",val confirmationStatus:String?=null,val attended:Boolean=false,val movie:String?=null,val theatre:String?=null,val screen:String?=null,val date:String?=null,val time:String?=null,val mobile:String?=null,val email:String?=null,val notification:String?=null,val cancellationReason:String?=null,val ticketCode:String?=null,val createdAt:String?=null,val updatedAt:String?=null)
private data class BookingCondition(val title:String,val description:String,val imageRes:Int)
private val BookingConditions=listOf(
    BookingCondition("Stay connected for confirmation","The cinema may contact you by call or email if your booking needs confirmation. Keep your contact details available.",R.drawable.terms_condition_contact),
    BookingCondition("Check your show before you confirm","Review the movie, cinema, date, showtime, and selected seats carefully. Your booking will be submitted for these details.",R.drawable.terms_condition_show),
    BookingCondition("Cancellation with clear timing","",R.drawable.terms_condition_refund),
    BookingCondition("Follow your cinema's rules","Follow the venue's entry, age, security, and other applicable rules. Arrive in time for the show.",R.drawable.terms_condition_venue)
)

interface AvApi {
    @POST("auth/otp/request") suspend fun requestOtp(@Body b:OtpPayload):ApiMessage
    @POST("auth/otp/verify") suspend fun verifyOtp(@Body b:VerifyPayload):OtpResult
    @POST("auth/otp/registration/request") suspend fun requestRegistrationOtp(@Body b:OtpPayload):ApiMessage
    @POST("auth/otp/registration/verify") suspend fun verifyRegistrationOtp(@Body b:VerifyPayload):RegistrationOtpResult
    @POST("auth/otp/registration/email/request") suspend fun requestRegistrationEmailOtp(@Body b:RegistrationEmailRequest):ApiMessage
    @POST("auth/otp/registration/email/verify") suspend fun verifyRegistrationEmailOtp(@Body b:RegistrationEmailVerifyRequest):RegistrationOtpResult
    @POST("auth/otp/register") suspend fun completeRegistration(@Body b:RegistrationCompletePayload):RegistrationOtpResult
    @GET("auth/me") suspend fun me():Me
    @PUT("auth/me") suspend fun updateMe(@Body b:UpdateProfilePayload):Me
    @GET("notifications") suspend fun notifications():com.google.gson.JsonElement
    @GET("movies") suspend fun movies(@Query("page") page:Int=0,@Query("size") size:Int=30,@Query("status") status:String?=null,@Query("title") title:String?=null):PageMovies
    @GET("movies/{id}") suspend fun movie(@Path("id") id:String):Movie
    @GET("shows") suspend fun shows(@Query("movieId") movieId:String?=null,@Query("theatreId") theatreId:String?=null,@Query("date") date:String?=null):List<Show>
    @GET("shows/{id}") suspend fun show(@Path("id") id:String):Show
    @GET("theatres") suspend fun theatres(@Query("page") page:Int=0,@Query("size") size:Int=100):PageTheatres
    @GET("theatres/{id}") suspend fun theatre(@Path("id") id:String):Theatre
    @GET("reviews") suspend fun reviews(@Query("movieId") movieId:String?=null):List<Review>
    @GET("shows/{id}/availability") suspend fun availability(@Path("id") id:String):com.google.gson.JsonElement
    @POST("bookings/seat-holds") suspend fun hold(@Body b:SeatIdsPayload):com.google.gson.JsonElement
    @POST("bookings") suspend fun createBooking(@Body b:BookingPayload):com.google.gson.JsonObject
    @GET("bookings/{id}") suspend fun booking(@Path("id") id:String):BookingRecord
    @POST("bookings/{id}/cancel") suspend fun cancelBooking(@Path("id") id:String,@Body b:CancelBookingPayload):BookingRecord
    @Streaming @GET("bookings/{id}/ticket-qr") suspend fun bookingQr(@Path("id") id:String):okhttp3.ResponseBody
    @GET("bookings/me") suspend fun bookings():List<BookingRecord>
    @GET("waiting-list") suspend fun waitingList():List<WaitingListEntry>
    @POST("waiting-list") suspend fun joinWaitingList(@Body b:WaitingListRequest):WaitingListEntry
    @POST("waiting-list/{id}/claim") suspend fun claimWaitingList(@Path("id") id:String):WaitingListEntry
    @DELETE("waiting-list/{id}") suspend fun cancelWaitingList(@Path("id") id:String)
}

private class RegistrationDraft {
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var mobile by mutableStateOf("")
    var mobileToken by mutableStateOf("")
    var emailToken by mutableStateOf("")
}

private object AvClient {
    @Volatile private var bearer:String?=null
    private var tokenStore:TokenStore?=null
    fun initialize(context:android.content.Context){tokenStore=TokenStore(context); bearer=tokenStore?.read()}
    fun currentToken()=bearer
    val api:AvApi by lazy {
        val http=OkHttpClient.Builder().connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS).readTimeout(8, java.util.concurrent.TimeUnit.SECONDS).writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS).addInterceptor { chain ->
            val req=chain.request().newBuilder().apply { bearer?.let { header("Authorization","Bearer $it") } }.build()
            chain.proceed(req)
        }.build()
        Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL.trimEnd('/')+"/").client(http).addConverterFactory(GsonConverterFactory.create()).build().create(AvApi::class.java)
    }
    fun token(value:String?) { bearer=value; tokenStore?.write(value) }
}

@Composable private fun AVApp() {
    val nav= rememberNavController()
    val registrationDraft= remember { RegistrationDraft() }
    var signedIn by remember { mutableStateOf(false) }
    NavHost(navController=nav,startDestination="splash") {
        composable("splash") { Splash { destination ->
            if(destination == "home") signedIn=true
            nav.navigate(if(destination == "login") "permissions" else destination) { popUpTo("splash") { inclusive=true }; launchSingleTop=true }
        } }
        composable("permissions") { AccessGate { nav.navigate("login") { popUpTo("permissions") { inclusive=true }; launchSingleTop=true } } }
        composable("login") { Login(onOtp={ email -> nav.navigate("otp/${android.net.Uri.encode(email)}") },onRegister={nav.navigate("register")}) }
        composable("otp/{email}",arguments=listOf(navArgument("email"){type=NavType.StringType})) { back -> Otp(back.arguments?.getString("email").orEmpty(), onSuccess={ signedIn=true; nav.navigate("home") { popUpTo("login") { inclusive=true } } }) }
        composable("register") { CreateAccount(registrationDraft,onBack={nav.popBackStack()},onVerifyMobile={nav.navigate("registration-otp/mobile")},onVerifyEmail={nav.navigate("registration-otp/email")},onCreated={signedIn=true; nav.navigate("home") { popUpTo("login") { inclusive=true } }}) }
        composable("registration-otp/{channel}",arguments=listOf(navArgument("channel"){type=NavType.StringType})) { back -> RegistrationOtp(registrationDraft,back.arguments?.getString("channel").orEmpty(),onVerified={nav.popBackStack()}) }
        composable("home") { Shell(nav,"home",signedIn){ Home(nav) } }
        composable("movies") { Shell(nav,"movies",signedIn){ Movies(nav) } }
        composable("what") { Shell(nav,"what",signedIn){ WhatIsAVScreen(nav) } }
        composable("about") { Shell(nav,"about",signedIn){ AboutScreen(nav) } }
        composable("profile") { Shell(nav,"profile",signedIn){ Profile(nav,onLogout={ signedIn=false; AvClient.token(null); nav.navigate("login") { popUpTo(0) } }) } }
        composable("notifications") { Shell(nav,"profile",signedIn){ NotificationsScreen() } }
        composable("notification-movie/{title}",arguments=listOf(navArgument("title"){type=NavType.StringType;defaultValue=""})){e->Shell(nav,"movies",signedIn){NotificationMovieRoute(nav,e.arguments?.getString("title").orEmpty())}}
        composable("waiting-list") { Shell(nav,"profile",signedIn){ WaitingListScreenCorrect(nav) } }
        composable("my-reviews") { Shell(nav,"profile",signedIn){ MyReviewsScreen() } }
        composable("help") { Shell(nav,"profile",signedIn){ HelpSupportScreen() } }
        composable("edit-profile") { Shell(nav,"profile",signedIn){ EditProfileScreen { nav.popBackStack() } } }
        composable("watchlist") { Shell(nav,"profile",signedIn){ SimpleAccountPage("Watchlist","Your saved movies will appear here when watchlist syncing is enabled.") } }
        composable("settings") { Shell(nav,"profile",signedIn){ SimpleAccountPage("Settings","Manage your Audience Verdict account preferences.") } }
        composable("details/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})){ e-> Shell(nav,"movies",signedIn){Details(nav,e.arguments?.getString("id").orEmpty())} }
        composable("shows/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})){ e-> Shell(nav,"movies",signedIn){ShowList(nav,e.arguments?.getString("id").orEmpty())} }
        composable("seats/{showId}",arguments=listOf(navArgument("showId"){type=NavType.StringType})){ e-> Shell(nav,"movies",signedIn){SeatInfo(nav,e.arguments?.getString("showId").orEmpty())} }
        composable("terms/{showId}?seats={seats}&expires={expires}",arguments=listOf(navArgument("showId"){type=NavType.StringType},navArgument("seats"){type=NavType.StringType;defaultValue=""},navArgument("expires"){type=NavType.StringType;defaultValue=""})){e->Shell(nav,"movies",signedIn){BookingTermsScreen(nav,e.arguments?.getString("showId").orEmpty(),e.arguments?.getString("seats").orEmpty(),e.arguments?.getString("expires").orEmpty())}}
        composable("booking-ticket/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})){e->Shell(nav,"profile",signedIn){BookingTicketScreen(nav,e.arguments?.getString("id").orEmpty())}}
        composable("bookings"){ Shell(nav,"profile",signedIn){ Bookings(nav) } }
        composable("review/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})){e-> Shell(nav,"movies",signedIn){ReviewPage(e.arguments?.getString("id").orEmpty(),nav)} }
        composable("trailer?url={url}&title={title}",arguments=listOf(navArgument("url"){type=NavType.StringType;defaultValue=""},navArgument("title"){type=NavType.StringType;defaultValue="Trailer"})){e->Shell(nav,"movies",signedIn){TrailerPlayerScreen(nav,e.arguments?.getString("url").orEmpty(),e.arguments?.getString("title")?:"Trailer")} }
    }
    val notificationIntent by PendingAvNotification.intent
    LaunchedEffect(notificationIntent) {
        val tapped=notificationIntent?:return@LaunchedEffect
        kotlinx.coroutines.withTimeoutOrNull(30_000) {
            while(nav.currentDestination?.route in setOf("splash","permissions","login","otp/{email}","register","registration-otp/{channel}")) delay(100)
        }
        if(nav.currentDestination?.route in setOf("splash","permissions","login","otp/{email}","register","registration-otp/{channel}")) {
            PendingAvNotification.intent.value=null
            return@LaunchedEffect
        }
        val destination=tapped.getStringExtra("av_notification_destination").orEmpty()
        when(destination) {
            "bookings"->nav.navigate("bookings"){launchSingleTop=true}
            "movie"->{
                val id=tapped.getStringExtra("av_movie_id").orEmpty()
                val title=tapped.getStringExtra("av_movie_title").orEmpty()
                if(id.isNotBlank()) nav.navigate("details/${Uri.encode(id)}"){launchSingleTop=true}
                else if(title.isNotBlank()) nav.navigate("notification-movie/${Uri.encode(title)}"){launchSingleTop=true}
                else nav.navigate("movies"){launchSingleTop=true}
            }
            "notifications"->nav.navigate("notifications"){launchSingleTop=true}
        }
        PendingAvNotification.intent.value=null
    }
    NotificationWatcher(signedIn)
}

@Composable private fun NotificationWatcher(enabled:Boolean) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    LaunchedEffect(enabled,owner) {
        if(!enabled)return@LaunchedEffect
        while(isActive) {
            if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                try { postNewLocalNotifications(context,parseApiList<AvNotification>(AvClient.api.notifications())) } catch(_:Throwable) { }
            }
            delay(60_000)
        }
    }
}

@Composable private fun Shell(nav:androidx.navigation.NavHostController, current:String, signedIn:Boolean, content:@Composable ()->Unit) {
    Scaffold(containerColor=Ink,bottomBar={ NavigationBar(containerColor=Color(0xFF0B0F11), tonalElevation=0.dp) {
        val items=listOf("home" to "Home","movies" to "Movies","what" to "What is AV?","about" to "About Us","profile" to "Profile")
        val icons=listOf(Icons.Default.Home,Icons.Default.Movie,Icons.Default.Star,Icons.Default.Info,Icons.Default.Person)
        items.forEachIndexed { i,(route,label)-> NavigationBarItem(selected=current==route,onClick={ nav.navigate(route){ launchSingleTop=true } },icon={ Icon(icons[i],label) },label={ Text(label,fontSize=9.sp) },colors=NavigationBarItemDefaults.colors(selectedIconColor=Red,selectedTextColor=Red,unselectedIconColor=Muted,unselectedTextColor=Muted,indicatorColor=Ink)) }
    }}) { pad -> Box(Modifier.padding(pad).fillMaxSize()){content()} }
}

@Composable private fun Splash(onReady:(String)->Unit) {
    var retry by remember { mutableStateOf(0) }
    var startupError by remember { mutableStateOf("") }
    val context=LocalContext.current
    LaunchedEffect(retry) {
        startupError = ""
        val started=SystemClock.elapsedRealtime()
        var destination:String?=null
        var error:String?=null
        if(!hasValidatedInternet(context)) {
            error="Network issue: no internet connection is available. Connect to the internet and retry."
        } else if(AvClient.currentToken().isNullOrBlank()) {
            destination="login"
        } else try {
            AvClient.api.me()
            destination="home"
        } catch (e:HttpException) {
            if(e.code()==401) { AvClient.token(null); destination="login" }
            else error=apiErrorMessage(e,context)
        } catch (e:Throwable) {
            error=apiErrorMessage(e,context)
        }
        val remaining=(2800-(SystemClock.elapsedRealtime()-started)).coerceAtLeast(0L)
        if(remaining>0) delay(remaining)
        if(destination!=null) onReady(destination!!) else startupError=error.orEmpty()
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        Image(painterResource(R.drawable.av_splash_background), contentDescription=null, modifier=Modifier.fillMaxSize(), contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000),Color(0x22000000),Color(0x66000000)))))
        Column(Modifier.fillMaxSize()
            .padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            .padding(horizontal=18.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(.72f)); SplashBrand(); Spacer(Modifier.weight(.40f)); LoadingAnimation()
            if (startupError.isNotBlank()) {
                Text(startupError,Modifier.padding(top=12.dp),color=Color(0xFFD1D1D1),fontSize=12.sp,textAlign=TextAlign.Center)
                TextButton(onClick={retry++}) { Text("Retry",color=Red) }
            }
            Spacer(Modifier.weight(.10f))
        }
    }
}

private val requiredPermissions = arrayOf(
    android.Manifest.permission.READ_CONTACTS,
    android.Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun hasRequiredPermissions(context:Context)=requiredPermissions.all {
    ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED
}

private fun hasValidatedInternet(context:Context):Boolean {
    val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network=manager.activeNetwork ?: return false
    val capabilities=manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun apiErrorMessage(error:Throwable,context:Context?=null):String {
    val cause=generateSequence(error){it.cause}.last()
    val http=(error as? HttpException) ?: (cause as? HttpException)
    if(http!=null) {
        val raw=runCatching{http.response()?.errorBody()?.string()}.getOrNull().orEmpty()
        val detail=runCatching {
            val json=com.google.gson.JsonParser.parseString(raw)
            if(json.isJsonObject) {
                val obj=json.asJsonObject
                listOf("message","detail","error","title").firstNotNullOfOrNull{key->obj.get(key)?.takeIf{it.isJsonPrimitive}?.asString}
                    ?: obj.get("fieldErrors")?.takeIf{!it.isJsonNull}?.toString()?.takeIf{it!="{}"&&it!="[]"}?.let{"Validation: $it"}
            } else null
        }.getOrNull()
        return "Server issue (HTTP ${http.code()}): ${detail?:"the service could not complete this request. Please try again later."}"
    }
    if(cause is java.net.UnknownHostException || (cause is java.io.IOException && context!=null && !hasValidatedInternet(context)))
        return "Network issue: the internet connection could not reach Audience Verdict. Check your connection and retry."
    if(cause is java.net.SocketTimeoutException || cause is java.net.ConnectException)
        return "Server issue: Audience Verdict's service did not respond. Please try again later."
    if(cause is java.io.IOException)
        return "Server issue: Audience Verdict's service could not be reached. Please try again later."
    return error.message?.takeIf{it.isNotBlank()} ?: "Request failed. Please try again."
}
private fun networkUnavailableMessage()="Network issue: no internet connection is available. Connect to the internet and retry."

@Composable private fun AccessGate(onAllowed:()->Unit) {
    val context=LocalContext.current
    val lifecycleOwner=LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(hasRequiredPermissions(context)) }
    var requested by remember { mutableStateOf(false) }
    val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        requested=true
        granted=hasRequiredPermissions(context)
    }
    DisposableEffect(lifecycleOwner) {
        val observer=LifecycleEventObserver { _, event ->
            if(event==Lifecycle.Event.ON_RESUME) {
                granted=hasRequiredPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState())
        .padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+28.dp,bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()+24.dp)
        .padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Logo(); Spacer(Modifier.height(34.dp)); Text("A couple of permissions",fontSize=22.sp,fontWeight=FontWeight.Bold)
        Text("Contacts and Location access are required to continue in this app.",Modifier.padding(top=8.dp,bottom=20.dp),color=Muted,textAlign=TextAlign.Center)
        listOf("Location" to "Allow Audience Verdict to access your device location.","Contacts" to "Allow Audience Verdict to access contacts on this device.").forEach { (title,detail) ->
            Row(Modifier.fillMaxWidth().padding(vertical=6.dp).background(Panel,RoundedCornerShape(12.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(if(title=="Location") Icons.Default.LocationOn else Icons.Default.Contacts,title,tint=Red)
                Column(Modifier.padding(start=14.dp)) { Text(title,fontWeight=FontWeight.SemiBold); Text(detail,color=Muted,fontSize=12.sp) }
            }
        }
        if(!granted && requested) Text("Both permissions are needed to continue. You can enable them in Android Settings.",Modifier.padding(top=12.dp),color=Red,fontSize=12.sp,textAlign=TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        if(!granted) Button(onClick={permissionLauncher.launch(requiredPermissions)},Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Red),shape=RoundedCornerShape(12.dp)) {
            Text(if(requested) "Allow Permissions" else "Continue")
        }
        if(!granted && requested) TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",context.packageName,null)))}) { Text("Open Settings",color=Muted) }
    }
    LaunchedEffect(granted) { if(granted) onAllowed() }
}

@Composable private fun SplashBrand() {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.av_logo),contentDescription="Audience Verdict",modifier=Modifier.width(300.dp).height(300.dp),contentScale=ContentScale.Fit)
        Spacer(Modifier.height(26.dp)); Text("WE SHOW. YOU DECIDE.",fontSize=12.sp,letterSpacing=4.sp,fontWeight=FontWeight.Medium,color=Color.White)
    }
}

@Composable private fun LoadingAnimation() {
    var item by remember { mutableStateOf(0) }; var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(if(visible) 1f else 0f,tween(220),label="loaderFade")
    val scale by animateFloatAsState(if(visible) 1f else .86f,tween(220),label="loaderScale")
    LaunchedEffect(Unit) { while(isActive) { delay(630); visible=false; delay(230); item=(item+1)%3; visible=true; delay(80) } }
    val transition=rememberInfiniteTransition(label="loadingProgress")
    val progress by transition.animateFloat(.24f,.72f,infiniteRepeatable(tween(950,easing=LinearEasing),RepeatMode.Reverse),label="progress")
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(116.dp,94.dp).alpha(alpha).scale(scale),contentAlignment=Alignment.Center) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x55FF1E2D),Color(0x18FF1E2D),Color.Transparent)),CircleShape))
            LoaderObject(item,Modifier.size(82.dp,78.dp))
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(210.dp).height(7.dp).background(Color(0x664F4F4F),RoundedCornerShape(50))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFFF746A),Red)),RoundedCornerShape(50)))
        }
        Text("L O A D I N G...",Modifier.padding(top=12.dp),fontSize=11.sp,letterSpacing=2.sp,color=Color(0xFFB9B9B9))
    }
}

@Composable private fun LoaderObject(kind:Int,modifier:Modifier=Modifier) {
    val image=when(kind%3){0->R.drawable.av_loader_microphone;1->R.drawable.av_loader_clapperboard;else->R.drawable.av_loader_ticket}
    val logoOffset=when(kind%3){0->Offset(10f,-5f);1->Offset(7f,18f);else->Offset(2f,0f)}
    val logoSize=when(kind%3){0->Modifier.size(18.dp,14.dp);1->Modifier.size(24.dp,16.dp);else->Modifier.size(21.dp,15.dp)}
    Box(modifier,contentAlignment=Alignment.Center) {
        Image(painterResource(image),contentDescription=when(kind%3){0->"Audience Verdict microphone";1->"Audience Verdict clapperboard";else->"Audience Verdict movie ticket"},modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
        Image(painterResource(R.drawable.av_logo),contentDescription="Audience Verdict logo",modifier=logoSize.offset(logoOffset.x.dp,logoOffset.y.dp),contentScale=ContentScale.Crop)
    }
}
@Composable private fun Logo() { Image(painterResource(R.drawable.av_logo),contentDescription="Audience Verdict logo",modifier=Modifier.size(176.dp),contentScale=ContentScale.Fit) }

@Composable private fun Login(onOtp:(String)->Unit,onRegister:()->Unit) {
    var email by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope= rememberCoroutineScope(); val context=LocalContext.current
    Box(Modifier.fillMaxSize().background(Ink)) {
        Image(painterResource(R.drawable.av_login_collage),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA050607),Color(0xB8080A0B),Color(0xEE080A0B)))))
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+24.dp,bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()+22.dp)
            .padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Logo(); Spacer(Modifier.height(34.dp))
            Text("Sign in with your Email",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            Text("We'll send you an OTP to continue",color=Color(0xFFC2C7CB),fontSize=14.sp,modifier=Modifier.padding(top=8.dp,bottom=22.dp))
            OutlinedTextField(email,{email=it;error=""},Modifier.fillMaxWidth().heightIn(min=54.dp),placeholder={Text("Enter your email",color=Color(0xFFA9B1B7))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),leadingIcon={Icon(Icons.Default.Email,"Email",tint=Color.White)},colors=fieldColors())
            if(error.isNotBlank()) Text(error,color=Red,modifier=Modifier.fillMaxWidth().padding(top=8.dp),fontSize=12.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick={
                if(!email.contains("@") || email.substringAfter('@').isBlank()) error="Enter a valid email address"
                else scope.launch {
                    busy=true; error=""
                    try {
                        if(!hasValidatedInternet(context)) error=networkUnavailableMessage()
                        else { val res=AvClient.api.requestOtp(OtpPayload(email.trim())); if(res.success==false) error=res.message?:"Could not send code" else onOtp(email.trim()) }
                    } catch(t:Throwable){error=apiErrorMessage(t,context)} finally{busy=false}
                }
            },Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Red),shape=RoundedCornerShape(11.dp),enabled=!busy) {
                if(busy) CircularProgressIndicator(Modifier.size(20.dp),color=Color.White)
                else Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){Text("Send OTP",fontWeight=FontWeight.Bold);Spacer(Modifier.width(10.dp));Icon(Icons.Default.ArrowForward,"Send OTP",Modifier.size(18.dp))}
            }
            Row(Modifier.fillMaxWidth().padding(vertical=22.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                HorizontalDivider(Modifier.weight(1f),color=Color(0xFF344047)); Text("  or continue with  ",fontSize=12.sp,color=Color(0xFFCED1D4)); HorizontalDivider(Modifier.weight(1f),color=Color(0xFF344047))
            }
            Text("Use your email to securely sign in to Audience Verdict.",fontSize=12.sp,color=Color(0xFFADB3B8),textAlign=TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("New to Audience Verdict? ",fontSize=13.sp,color=Color(0xFFCED1D4))
                Text("Create Account",fontSize=13.sp,color=Red,fontWeight=FontWeight.Bold,modifier=Modifier.clickable{onRegister()})
            }
        }
    }
}
@Composable private fun CreateAccount(draft:RegistrationDraft,onBack:()->Unit,onVerifyMobile:()->Unit,onVerifyEmail:()->Unit,onCreated:()->Unit) {
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope= rememberCoroutineScope()
    val context=LocalContext.current
    val emailValid=draft.email.trim().matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    val mobileDigits=draft.mobile.filter(Char::isDigit)
    val mobileValid=mobileDigits.length in 10..15
    val ready=draft.name.trim().isNotEmpty() && emailValid && mobileValid && draft.mobileToken.isNotBlank() && draft.emailToken.isNotBlank()
    Box(Modifier.fillMaxSize().background(Ink)) {
        Image(painterResource(R.drawable.av_login_collage),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xDD050607),Color(0xF2080A0B),Color(0xFF080A0B)))))
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+10.dp,bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()+20.dp)
            .padding(horizontal=22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack) { Icon(Icons.Default.ArrowBack,"Back") }
                Text("Create Your Account",Modifier.weight(1f),fontSize=21.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }
            Logo()
            Text("Join Audience Verdict and be part of the movie community",Modifier.padding(top=4.dp,bottom=18.dp),color=Muted,textAlign=TextAlign.Center,fontSize=13.sp)
            OutlinedTextField(value=draft.name,onValueChange={draft.name=it;error=""},modifier=Modifier.fillMaxWidth(),label={Text("Full name")},singleLine=true,leadingIcon={Icon(Icons.Default.Person,"Name")},colors=fieldColors())
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=draft.email,onValueChange={draft.email=it;draft.emailToken="";error=""},modifier=Modifier.weight(1f),label={Text("Email address")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),leadingIcon={Icon(Icons.Default.Email,"Email")},trailingIcon={if(draft.emailToken.isNotBlank()) Icon(Icons.Default.CheckCircle,"Email verified",tint=Color(0xFF23D18B))},colors=fieldColors())
                if(draft.emailToken.isBlank()) TextButton(onClick={
                    if(!emailValid) error="Enter a valid email address first"
                    else if(draft.mobileToken.isBlank()) error="Verify your mobile number first"
                    else scope.launch { busy=true;error="";try { if(!hasValidatedInternet(context)) error=networkUnavailableMessage() else { val r=AvClient.api.requestRegistrationEmailOtp(RegistrationEmailRequest(draft.mobileToken,draft.email.trim())); if(r.success==false) error=r.message?:"Unable to send the email code" else onVerifyEmail() } } catch(t:Throwable){error=apiErrorMessage(t,context)} finally{busy=false} }
                },enabled=!busy&&draft.mobileToken.isNotBlank()) { Text("Verify",color=if(draft.mobileToken.isNotBlank()) Red else Muted,fontWeight=FontWeight.Bold) }
            }
            if(draft.mobileToken.isBlank()) Text("Verify your mobile first to unlock email verification.",Modifier.fillMaxWidth().padding(start=4.dp,top=3.dp),color=Muted,fontSize=11.sp)
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=draft.mobile,onValueChange={draft.mobile=it.filter{c->c.isDigit()||c=='+'||c==' '||c=='-'}.take(18);draft.mobileToken="";draft.emailToken="";error=""},modifier=Modifier.weight(1f),label={Text("Mobile number")},placeholder={Text("+91 98765 43210")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),leadingIcon={Icon(Icons.Default.Phone,"Mobile")},trailingIcon={if(draft.mobileToken.isNotBlank()) Icon(Icons.Default.CheckCircle,"Mobile verified",tint=Color(0xFF23D18B))},colors=fieldColors())
                if(draft.mobileToken.isBlank()) TextButton(onClick={
                    if(!mobileValid) error="Enter a valid mobile number first"
                    else scope.launch { busy=true;error="";try { if(!hasValidatedInternet(context)) error=networkUnavailableMessage() else { val destination=if(draft.mobile.trim().startsWith("+")) "+${mobileDigits}" else "+91${mobileDigits}"; draft.mobile=destination; val r=AvClient.api.requestRegistrationOtp(OtpPayload(destination)); if(r.success==false) error=r.message?:"Unable to send the WhatsApp code" else onVerifyMobile() } } catch(t:Throwable){error=apiErrorMessage(t,context)} finally{busy=false} }
                },enabled=!busy) { Text("Verify",color=Red,fontWeight=FontWeight.Bold) }
            }
            Text("A WhatsApp OTP will be sent to your mobile number.",Modifier.fillMaxWidth().padding(start=4.dp,top=3.dp),color=Muted,fontSize=11.sp)
            if(error.isNotBlank()) Text(error,Modifier.fillMaxWidth().padding(top=12.dp),color=Red,fontSize=12.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick={scope.launch { busy=true;error="";try { if(!hasValidatedInternet(context)) error=networkUnavailableMessage() else { val r=AvClient.api.completeRegistration(RegistrationCompletePayload(draft.mobileToken,draft.name.trim(),draft.email.trim(),draft.mobile,draft.emailToken)); val token=r.accessToken; if(token.isNullOrBlank()) error="Your account was created, but sign-in could not be started. Please sign in with your email." else {AvClient.token(token);onCreated()} } } catch(t:Throwable){error=apiErrorMessage(t,context)} finally{busy=false} } },modifier=Modifier.fillMaxWidth().height(52.dp),enabled=ready&&!busy,colors=ButtonDefaults.buttonColors(containerColor=Red,disabledContainerColor=Color(0xFF493638)),shape=RoundedCornerShape(12.dp)) {
                if(busy) CircularProgressIndicator(Modifier.size(19.dp),color=Color.White) else Text("Create Account",fontWeight=FontWeight.Bold)
            }
            Text(if(ready) "Both your email and mobile number are verified." else "Verify your email and mobile number to continue.",Modifier.padding(top=12.dp),fontSize=12.sp,color=Muted,textAlign=TextAlign.Center)
        }
    }
}

@Composable private fun RegistrationOtp(draft:RegistrationDraft,channel:String,onVerified:()->Unit) {
    val isEmail=channel=="email"
    val destination=if(isEmail) draft.email else draft.mobile
    var otp by remember(channel,destination){mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    val scope= rememberCoroutineScope()
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().background(Ink).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onVerified){Icon(Icons.Default.ArrowBack,"Back")};Spacer(Modifier.weight(1f))}
        Logo();Spacer(Modifier.height(26.dp));Text(if(isEmail) "Verify Your Email" else "Verify Your Mobile Number",fontSize=23.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        Text(if(isEmail) "We sent a 6-digit code to $destination" else "We sent a WhatsApp OTP to $destination",Modifier.padding(top=10.dp,bottom=22.dp),color=Muted,textAlign=TextAlign.Center)
        OutlinedTextField(otp,{otp=it.filter(Char::isDigit).take(6);error=""},Modifier.fillMaxWidth(),label={Text("6-digit code")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,colors=fieldColors())
        if(error.isNotBlank()) Text(error,Modifier.padding(top=10.dp),color=Red,fontSize=12.sp,textAlign=TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick={scope.launch { busy=true;error="";try { if(!hasValidatedInternet(context)) error=networkUnavailableMessage() else if(isEmail) { val r=AvClient.api.verifyRegistrationEmailOtp(RegistrationEmailVerifyRequest(draft.mobileToken,draft.email.trim(),otp)); if(r.registrationToken.isNullOrBlank()) error="The server did not return an email verification token." else {draft.emailToken=r.registrationToken;onVerified()} } else { val r=AvClient.api.verifyRegistrationOtp(VerifyPayload(draft.mobile,otp)); if(r.registrationToken.isNullOrBlank()) error="The server did not return a mobile registration token." else {draft.mobileToken=r.registrationToken;onVerified()} } } catch(t:Throwable){error=apiErrorMessage(t,context)} finally{busy=false} }},Modifier.fillMaxWidth().height(52.dp),enabled=otp.length==6&&!busy,colors=ButtonDefaults.buttonColors(containerColor=Red)) { if(busy) CircularProgressIndicator(Modifier.size(19.dp),color=Color.White) else Text("Verify",fontWeight=FontWeight.Bold) }
        TextButton(onClick={scope.launch {busy=true;error="";try {if(!hasValidatedInternet(context)) error=networkUnavailableMessage() else if(isEmail) {val r=AvClient.api.requestRegistrationEmailOtp(RegistrationEmailRequest(draft.mobileToken,draft.email.trim()));if(r.success==false)error=r.message?:"Unable to resend code"} else {val r=AvClient.api.requestRegistrationOtp(OtpPayload(draft.mobile));if(r.success==false)error=r.message?:"Unable to resend code"}}catch(t:Throwable){error=apiErrorMessage(t,context)}finally{busy=false}}}){Text("Resend code",color=Muted)}
    }
}

@Composable private fun Otp(email:String,onSuccess:()->Unit) {
    var otp by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope= rememberCoroutineScope();val context=LocalContext.current
    Column(Modifier.fillMaxSize().background(Ink).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){ Logo(); Spacer(Modifier.height(32.dp)); Text("Verify OTP",fontSize=24.sp,fontWeight=FontWeight.Bold); Text("Code sent to $email",color=Muted,modifier=Modifier.padding(10.dp)); OutlinedTextField(otp,{otp=it.filter(Char::isDigit).take(6)},Modifier.fillMaxWidth(),label={Text("6-digit code")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,colors=fieldColors()); if(error.isNotBlank()) Text(error,color=Red); Spacer(Modifier.height(16.dp)); Button(onClick={scope.launch {busy=true; try { val result=AvClient.api.verifyOtp(VerifyPayload(email,otp)); if(!result.accessToken.isNullOrBlank()){AvClient.token(result.accessToken); AvClient.api.me(); onSuccess()} else error="Registration is required for this account. Registration flow is not available yet." }catch(t:Throwable){error=apiErrorMessage(t,context)}finally{busy=false}}},Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Red),enabled=otp.length==6&&!busy){if(busy) CircularProgressIndicator(Modifier.size(18.dp),color=Color.White) else Text("Verify")}; TextButton(onClick={scope.launch {try{AvClient.api.requestOtp(OtpPayload(email))}catch(t:Throwable){error=apiErrorMessage(t,context)}}}){Text("Resend code",color=Muted)} }
}
@Composable private fun fieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=Red,unfocusedBorderColor=Color(0xFF43505A),focusedLabelColor=Red,cursorColor=Red,focusedTextColor=Color.White,unfocusedTextColor=Color.White)

@Composable private fun Home(nav:androidx.navigation.NavHostController) {
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(true) }
    val context=LocalContext.current
    LaunchedEffect(Unit){try{movies=AvClient.api.movies().content}catch(t:Throwable){error=apiErrorMessage(t,context)}finally{busy=false}}
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(bottom=20.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("AV",color=Red,fontSize=30.sp,fontWeight=FontWeight.Black); Spacer(Modifier.width(16.dp)); Text("?  Hyderabad",Modifier.weight(1f),fontSize=14.sp); Icon(Icons.Default.Search,"Search",Modifier.clickable{nav.navigate("movies")}); Spacer(Modifier.width(16.dp)); Icon(Icons.Default.Notifications,"Notifications",tint=Red)}
        if(movies.isNotEmpty()) { val featured=movies.firstOrNull{!it.backdropUrl.isNullOrBlank()}?:movies.first(); Box(Modifier.fillMaxWidth().height(250.dp).padding(horizontal=14.dp).clickable{nav.navigate("details/${featured.id}")}) { Poster(featured.backdropUrl?:featured.posterUrl,Modifier.fillMaxSize()); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Ink.copy(alpha=.96f)),startY=90f))); Column(Modifier.align(Alignment.BottomStart).padding(20.dp)){Text(featured.title,fontSize=26.sp,fontWeight=FontWeight.Black); Text(featured.genre?.joinToString(" • ")?:"",color=Muted); Button(onClick={nav.navigate("details/${featured.id}")},colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("Book Now")}} } }
        SectionTitle("Trending Movies",onClick={nav.navigate("movies")}); if(busy) Busy() else if(error.isNotBlank()) ErrorCard(error) else PosterRow(movies.filter{it.status!="UPCOMING"}.ifEmpty{movies},nav)
        SectionTitle("Coming Soon",onClick={nav.navigate("movies")}); PosterRow(movies.filter{it.status=="UPCOMING"},nav)
    }
}
@Composable private fun SectionTitle(title:String,onClick:()->Unit){Row(Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=18.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),fontSize=19.sp,fontWeight=FontWeight.Bold); Text("See All ›",Modifier.clickable{onClick()},color=Red,fontSize=13.sp)}}
@Composable private fun PosterRow(list:List<Movie>,nav:androidx.navigation.NavHostController){LazyRow(contentPadding=PaddingValues(horizontal=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){items(list){m-> Column(Modifier.width(126.dp).clickable{nav.navigate("details/${m.id}")}){Poster(m.posterUrl,Modifier.fillMaxWidth().height(176.dp)); Text(m.title,maxLines=1,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=6.dp)); Text(m.genre?.firstOrNull()?:m.genres?.firstOrNull()?:"",color=Muted,fontSize=11.sp,maxLines=1)}}}}
@Composable private fun Poster(url:String?,modifier:Modifier=Modifier){
    val context=LocalContext.current
    var source by remember(url){mutableStateOf(imageUrlCandidates(url.orEmpty()).firstOrNull().orEmpty())}
    var failed by remember(url){mutableStateOf(source.isBlank())}
    var lookedUp by remember(url){mutableStateOf(false)}
    LaunchedEffect(source,failed,url) {
        if(failed&&!lookedUp&&!url.isNullOrBlank()) {
            lookedUp=true
            val pageImage=resolvePosterPageImage(url)
            if(!pageImage.isNullOrBlank()&&pageImage!=source){source=pageImage;failed=false}
        }
    }
    Box(modifier.background(Brush.linearGradient(listOf(Panel2,Color(0xFF321115)))),contentAlignment=Alignment.Center) {
        if(!failed&&source.isNotBlank()) AsyncImage(model=coil.request.ImageRequest.Builder(context).data(source).addHeader("User-Agent","AudienceVerdictAndroid/1.0 (image preview loader)").addHeader("Referer",imageReferer(source)).crossfade(true).build(),contentDescription="Movie artwork",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop,onError={failed=true})
        if(failed) Text("AV",fontSize=34.sp,fontWeight=FontWeight.Black,color=Red)
    }
}

private suspend fun resolvePosterPageImage(link:String):String? = withContext(Dispatchers.IO) {
    try {
        val normalized=normalizeWebUrl(link)?:return@withContext null
        val connection=java.net.URL(normalized).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout=7000;connection.readTimeout=7000;connection.instanceFollowRedirects=true
        connection.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
        connection.setRequestProperty("Accept","text/html,application/xhtml+xml,image/avif,image/webp,image/apng,*/*;q=0.8")
        connection.connect()
        if(connection.contentType?.startsWith("image/",true)==true) return@withContext connection.url.toString()
        val pageUrl=connection.url.toString()
        val html=connection.inputStream.bufferedReader(Charsets.UTF_8).use{reader->val out=StringBuilder();val buffer=CharArray(4096);var count:Int;while(reader.read(buffer).also{count=it}>=0&&out.length<1_000_000)out.append(buffer,0,minOf(count,1_000_000-out.length));out.toString()}
        connection.disconnect()
        val tags=Regex("(?is)<meta\\b[^>]*>").findAll(html)
        val image=tags.mapNotNull{match->
            val tag=match.value
            val property=Regex("(?is)(?:property|name)\\s*=\\s*[\\\"']([^\\\"']+)").find(tag)?.groupValues?.get(1)?.lowercase()
            if(property !in setOf("og:image","og:image:url","twitter:image","twitter:image:src")) null
            else Regex("(?is)content\\s*=\\s*[\\\"']([^\\\"']+)").find(tag)?.groupValues?.get(1)
        }.firstOrNull()?.let{android.text.Html.fromHtml(it,android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()}
        if(image.isNullOrBlank()) null else java.net.URI(pageUrl).resolve(image).toString()
    } catch(_:Throwable) { null }
}
@Composable private fun Busy(){Box(Modifier.fillMaxWidth().height(120.dp),Alignment.Center){CircularProgressIndicator(color=Red)}}
@Composable private fun ErrorCard(text:String){Text("Couldn't load from Audience Verdict API\n$text",Modifier.fillMaxWidth().padding(16.dp).background(Panel,RoundedCornerShape(12.dp)).padding(16.dp),color=Muted)}

@Composable private fun Movies(nav:androidx.navigation.NavHostController){var q by remember{mutableStateOf("")};var filter by remember{mutableStateOf("All")};var movies by remember{mutableStateOf<List<Movie>>(emptyList())};var error by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};val context=LocalContext.current
    LaunchedEffect(q,filter){busy=true;error="";try{movies=AvClient.api.movies(status=when(filter){"Now Showing"->"ACTIVE";"Coming Soon"->"UPCOMING";else->null},title=q.ifBlank{null}).content}catch(t:Throwable){error=apiErrorMessage(t,context)}finally{busy=false}}
    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal=16.dp)){Row(Modifier.padding(top=14.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Text("Movies",Modifier.weight(1f),fontSize=24.sp,fontWeight=FontWeight.Bold);Icon(Icons.Default.Search,"Search")}; OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),placeholder={Text("Search movies, actors, directors…",color=Muted)},singleLine=true,colors=fieldColors(),leadingIcon={Icon(Icons.Default.Search,"Search")}); Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("All","Now Showing","Coming Soon","Top Rated").forEach{AssistChip(onClick={filter=it},label={Text(it)},colors=AssistChipDefaults.assistChipColors(containerColor=if(filter==it) Red else Panel, labelColor=Color.White))}}
        if(busy) Busy() else if(error.isNotBlank()) ErrorCard(error) else LazyVerticalGridCompat(movies,nav)
    }
}
@Composable private fun LazyVerticalGridCompat(movies:List<Movie>,nav:androidx.navigation.NavHostController){androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns=androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(9.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(movies.size){i->val m=movies[i];Column(Modifier.clickable{nav.navigate("details/${m.id}")}){Poster(m.posterUrl,Modifier.fillMaxWidth().height(150.dp));Text(m.title,maxLines=1,fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text(m.genre?.firstOrNull()?:m.genres?.firstOrNull()?:"",maxLines=1,fontSize=10.sp,color=Muted)}}}}

@Composable private fun Details(nav:androidx.navigation.NavHostController,id:String) {
    var movie by remember(id){mutableStateOf<Movie?>(null)}
    var reviews by remember(id){mutableStateOf<List<Review>>(emptyList())}
    var error by remember(id){mutableStateOf("")}
    var reviewError by remember(id){mutableStateOf("")}
    var tab by remember(id){mutableStateOf("Overview")}
    var saved by remember(id){mutableStateOf(false)}
    var bookingCity by remember(id){mutableStateOf("")}
    var canBookInCity by remember(id){mutableStateOf(false)}
    var cityCheckComplete by remember(id){mutableStateOf(false)}
    var bookingNotice by remember(id){mutableStateOf("")}
    val context=LocalContext.current
    LaunchedEffect(id) {
        try { movie=AvClient.api.movie(id) } catch(t:Throwable) { error=apiErrorMessage(t,context) }
        try { reviews=AvClient.api.reviews(id) } catch(t:Throwable) { reviewError=apiErrorMessage(t,context) }
    }
    LaunchedEffect(id) {
        try {
            val city=resolveDeviceCity(context)
            if(city.isNullOrBlank()) bookingNotice="Turn on device location to verify your city before booking."
            else {
                bookingCity=city
                val listings=AvClient.api.shows(movieId=id)
                val venues=AvClient.api.theatres().content.associateBy{it.id}
                val today=java.time.LocalDate.now()
                canBookInCity=listings.any{show->
                    val future=showDateKey(show.date)?.let{runCatching{!java.time.LocalDate.parse(it).isBefore(today)}.getOrDefault(false)}==true
                    future&&show.status.uppercase() !in setOf("CANCELLED","EXPIRED","INACTIVE","SOLD_OUT")&&theatreMatchesCity(venues[show.theatreId],city)
                }
                if(!canBookInCity)bookingNotice="No showtimes for this movie are available in $city."
            }
        } catch(t:Throwable) { bookingNotice="Could not verify local showtimes. Booking is disabled until location and show availability can be checked." }
        finally { cityCheckComplete=true }
    }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState())) {
        val m=movie
        if(m==null) { if(error.isNotBlank()) ErrorCard(error) else Busy() }
        else {
            val trailer=m.trailerUrl?:m.externalTrailerUrl?:m.media.orEmpty().firstOrNull{it.mediaType.contains("TRAILER")||it.mediaType=="TEASER"}?.let{it.embedUrl?:it.mediaUrl}
            val playTrailer={ if(!trailer.isNullOrBlank()) nav.navigate("trailer?url=${Uri.encode(trailer)}&title=${Uri.encode(m.title)}") }
            Box(Modifier.fillMaxWidth().height(465.dp)) {
                Poster(m.posterUrl?:m.posterImages?.firstOrNull(),Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x55000000),Color.Transparent,Color(0xCC050607),Ink),startY=0f)))
                Row(Modifier.fillMaxWidth().padding(top=4.dp,start=8.dp,end=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={nav.popBackStack()}) { Icon(Icons.Default.ArrowBack,"Back",tint=Color.White) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick={saved=!saved}) { Icon(if(saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Watchlist",tint=Red) }
                    IconButton(onClick={val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"${m.title} — Audience Verdict")};context.startActivity(Intent.createChooser(send,"Share movie"))}) { Icon(Icons.Default.Share,"Share",tint=Color.White) }
                }
                Column(Modifier.align(Alignment.Center).padding(top=30.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    IconButton(onClick=playTrailer,modifier=Modifier.size(62.dp).background(Color(0x66000000),CircleShape).border(1.dp,Color.White,CircleShape)) { Icon(Icons.Default.PlayArrow,"Watch trailer",tint=Color.White,modifier=Modifier.size(38.dp)) }
                    Text(if(trailer.isNullOrBlank()) "Trailer unavailable" else "Watch Trailer",Modifier.padding(top=5.dp),fontSize=12.sp,color=Color.White)
                }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal=18.dp,vertical=16.dp)) {
                    AssistChip(onClick={},label={Text(when(m.status?.uppercase()){ "ACTIVE"->"NOW SCREENING";"UPCOMING"->"COMING SOON";else->m.releaseStatus?.replace('_',' ')?:"MOVIE"},fontWeight=FontWeight.Bold,fontSize=10.sp)},colors=AssistChipDefaults.assistChipColors(containerColor=Red, labelColor=Color.White))
                    Text(m.title,Modifier.padding(top=4.dp),fontSize=29.sp,fontWeight=FontWeight.Black,lineHeight=34.sp)
                    if(!m.tagline.isNullOrBlank()) Text(m.tagline,Modifier.padding(top=4.dp),fontSize=13.sp,color=Color(0xFFD4D7DA))
                    Text(listOfNotNull(m.certification,m.language?:m.originalLanguage,(m.durationMinutes?:m.duration)?.let{"${it} min"},m.releaseDate?.take(4)).joinToString("  ·  "),Modifier.padding(top=8.dp),fontSize=12.sp,color=Color(0xFFD0D4D8))
                    Text(if(!cityCheckComplete)"Checking showtimes in your city..." else if(canBookInCity)"Showtimes available in $bookingCity" else bookingNotice,Modifier.padding(top=8.dp),fontSize=10.sp,color=if(canBookInCity)Color(0xFF42D392) else Muted,maxLines=2)
                    Row(Modifier.fillMaxWidth().padding(top=13.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Button(onClick={nav.navigate("shows/${m.id}")},modifier=Modifier.weight(1f).height(50.dp),enabled=canBookInCity,colors=ButtonDefaults.buttonColors(containerColor=Red,disabledContainerColor=Panel2,disabledContentColor=Muted),shape=RoundedCornerShape(11.dp)) { Text("Book Tickets",fontWeight=FontWeight.Bold);Icon(Icons.Default.ArrowForward,"Book",Modifier.padding(start=6.dp).size(18.dp)) }
                        OutlinedButton(onClick=playTrailer,modifier=Modifier.height(50.dp),enabled=!trailer.isNullOrBlank(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White),border=ButtonDefaults.outlinedButtonBorder.copy(brush=Brush.linearGradient(listOf(Color(0xFF66717A),Color(0xFF66717A))))) { Icon(Icons.Default.PlayArrow,"Trailer");Text("Trailer",Modifier.padding(start=4.dp)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=7.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                (m.genre?:m.genres.orEmpty()).take(5).forEach{AssistChip(onClick={},label={Text(it,fontSize=11.sp)},colors=AssistChipDefaults.assistChipColors(containerColor=Panel2,labelColor=Color.White))}
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=4.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                listOf("Overview","Cast & Crew","Media","Reviews").forEach { label ->
                    Column(Modifier.clickable{tab=label}.padding(horizontal=12.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(label,fontSize=12.sp,color=if(tab==label)Color.White else Muted,fontWeight=if(tab==label)FontWeight.SemiBold else FontWeight.Normal)
                        Spacer(Modifier.height(8.dp)); Box(Modifier.width(52.dp).height(2.dp).background(if(tab==label)Red else Color.Transparent,RoundedCornerShape(2.dp)))
                    }
                }
            }
            HorizontalDivider(color=Color(0xFF273137))
            when(tab) {
                "Overview" -> MovieOverview(m)
                "Cast & Crew" -> MovieCastAndCrew(m)
                "Media" -> MovieMediaSection(m){url,title->nav.navigate("trailer?url=${Uri.encode(url)}&title=${Uri.encode(title)}")}
                "Reviews" -> MovieReviews(m,reviews,reviewError){nav.navigate("review/${m.id}")}
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun MovieOverview(movie:Movie) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("About the Movie",fontSize=18.sp,fontWeight=FontWeight.Bold)
        Text(movie.synopsis?:movie.description?:"No movie synopsis is available yet.",Modifier.padding(top=8.dp),color=Color(0xFFC2C8CD),fontSize=13.sp,lineHeight=20.sp)
        val details=listOf(
            "Release Date" to movie.releaseDate?.let(::displayMovieDate),
            "Announcement Date" to movie.announcementDate?.let(::displayMovieDate),
            "Runtime" to (movie.durationMinutes?:movie.duration)?.let{"$it min"},
            "Language" to (movie.language?:movie.originalLanguage?:movie.languages?.joinToString(", ")),
            "Certification" to movie.certification,
            "Director" to movie.director,
            "Production Company" to (movie.productionCompanies?.joinToString(", ")?:movie.production),
            "Official Website" to movie.officialWebsite
        ).filter{!it.second.isNullOrBlank()}
        if(details.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            details.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach { (label,value) -> Column(Modifier.weight(1f).padding(bottom=8.dp).background(Panel,RoundedCornerShape(10.dp)).padding(12.dp)) { Text(label,fontSize=10.sp,color=Muted);Text(value.orEmpty(),fontSize=12.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=4.dp)) } }
                if(row.size==1) Spacer(Modifier.weight(1f))
            } }
        }
    }
}

@Composable private fun MovieCastAndCrew(movie:Movie) {
    val cast=movie.castCredits.orEmpty().filter{it.creditType.isNullOrBlank()||it.creditType.equals("CAST",true)}.sortedByDescending{it.isMainCast}
    val crew=movie.crewByDepartment.orEmpty()
    var selectedProfile by remember(movie.id){mutableStateOf<Pair<MovieCredit,Boolean>?>(null)}
    Column(Modifier.fillMaxWidth().padding(vertical=15.dp)) {
        Text("Cast & Characters",Modifier.padding(horizontal=16.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
        if(cast.isNotEmpty()) LazyRow(contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(cast.size) { i -> val credit=cast[i]; Column(Modifier.width(108.dp)) {
                Box(Modifier.fillMaxWidth().height(132.dp).background(Panel2,RoundedCornerShape(10.dp)).clickable{selectedProfile=credit to false},contentAlignment=Alignment.Center) {
                    CastPortrait(
                        listOf(credit.characterImageUrl,credit.person?.profileImageUrl,credit.person?.officialWebsite)+credit.person?.socialLinks.orEmpty().map{it.profileUrl},
                        credit.person?.fullName?:credit.characterName?:"Cast member"
                    )
                }
                Text(credit.person?.fullName?:credit.characterName?:"Cast member",Modifier.padding(top=6.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                Text(credit.characterName?:credit.roleTitle?:"",fontSize=10.sp,color=Muted,maxLines=2)
            } }
        } else if(!movie.cast.isNullOrEmpty()) {
            LazyRow(contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){items(movie.cast.size){i->val name=movie.cast[i];Column(Modifier.width(110.dp)){Box(Modifier.fillMaxWidth().height(120.dp).background(Panel2,RoundedCornerShape(10.dp)).clickable{selectedProfile=MovieCredit(roleTitle=name) to false},contentAlignment=Alignment.Center){Icon(Icons.Default.Person,"Cast",tint=Muted,modifier=Modifier.size(34.dp))};Text(name,Modifier.padding(top=7.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=2)}}}
        } else Text("Cast details will appear here when available.",Modifier.padding(16.dp),color=Muted,fontSize=13.sp)
        if(crew.isNotEmpty()) {
            Text("Crew",Modifier.padding(start=16.dp,top=10.dp,bottom=8.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
            crew.forEach{(department,people)-> people.forEach { credit -> Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=4.dp).background(Panel,RoundedCornerShape(10.dp)).clickable{selectedProfile=credit to true}.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Panel2,CircleShape),contentAlignment=Alignment.Center){CastPortrait(listOf(credit.person?.profileImageUrl,credit.person?.officialWebsite)+credit.person?.socialLinks.orEmpty().map{it.profileUrl},credit.person?.fullName?:credit.roleTitle?:department)}
                Column(Modifier.padding(start=12.dp)){Text(credit.person?.fullName?:credit.roleTitle?:department,fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text(credit.roleTitle?:credit.department?:department,color=Muted,fontSize=11.sp)}
            } } }
        } else if(!movie.director.isNullOrBlank()||!movie.production.isNullOrBlank()) {
            Text("Crew",Modifier.padding(start=16.dp,top=10.dp,bottom=8.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
            listOfNotNull(movie.director?.let{"Director" to it},movie.production?.let{"Production" to it}).forEach{(role,name)->Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=4.dp).background(Panel,RoundedCornerShape(10.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Movie,role,tint=Red);Column(Modifier.padding(start=12.dp)){Text(name,fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text(role,color=Muted,fontSize=11.sp)}}}
        }
    }
    selectedProfile?.let{(credit,isCrew)->CastProfilePopup(credit,isCrew){selectedProfile=null}}
}

/** Loads direct image URLs first, then tries OpenGraph/Twitter preview images for page URLs. */
@Composable private fun CastPortrait(urls:List<String?>, name:String) {
    val context=LocalContext.current
    val candidates=remember(urls){urls.flatMap{imageUrlCandidates(it.orEmpty())}.distinct()}
    var candidateIndex by remember(candidates){mutableIntStateOf(0)}
    var source by remember(candidates){mutableStateOf(candidates.firstOrNull().orEmpty())}
    var failed by remember(candidates){mutableStateOf(source.isBlank())}
    val resolvedCandidates=remember(candidates){mutableStateListOf<Int>()}

    LaunchedEffect(candidateIndex,failed,candidates) {
        if(!failed || candidateIndex !in candidates.indices) return@LaunchedEffect
        val currentIndex=candidateIndex
        if(currentIndex !in resolvedCandidates) {
            resolvedCandidates.add(currentIndex)
            val preview=resolvePosterPageImage(candidates[currentIndex])
            if(!preview.isNullOrBlank() && preview!=source) {
                source=preview
                failed=false
                return@LaunchedEffect
            }
        }
        if(currentIndex+1<candidates.size) {
            candidateIndex=currentIndex+1
            source=candidates[candidateIndex]
            failed=false
        }
    }

    Box(Modifier.fillMaxSize().background(Panel2),contentAlignment=Alignment.Center) {
        if(!failed && source.isNotBlank()) {
            AsyncImage(
                model=coil.request.ImageRequest.Builder(context).data(source).addHeader("User-Agent","AudienceVerdictAndroid/1.0 (image preview loader)").addHeader("Referer",imageReferer(source)).crossfade(true).build(),
                contentDescription="$name portrait",
                modifier=Modifier.fillMaxSize(),
                contentScale=ContentScale.Crop,
                onError={ failed=true }
            )
        } else {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.Person,"$name",tint=Muted,modifier=Modifier.size(36.dp))
                Text(name.split(" ").mapNotNull{it.firstOrNull()?.toString()}.take(2).joinToString(""),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable private fun CastProfilePopup(credit:MovieCredit,isCrew:Boolean,onDismiss:()->Unit) {
    val name=credit.person?.fullName?:credit.characterName?:credit.roleTitle?:"Profile"
    val role=if(isCrew) credit.roleTitle?:credit.department?:"Crew" else credit.characterName?:credit.roleTitle?.takeIf{it!=name}?:"Cast"
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(20.dp)).padding(22.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(116.dp).background(Panel2,CircleShape),contentAlignment=Alignment.Center) {
                CastPortrait(listOf(credit.characterImageUrl,credit.person?.profileImageUrl,credit.person?.officialWebsite)+credit.person?.socialLinks.orEmpty().map{it.profileUrl},name)
            }
            Text(name,Modifier.padding(top=14.dp),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center)
            Text(role,Modifier.padding(top=4.dp),fontSize=13.sp,color=Red,textAlign=TextAlign.Center)
            if(!credit.department.isNullOrBlank() && credit.department!=role) Text(credit.department,Modifier.padding(top=3.dp),fontSize=12.sp,color=Muted,textAlign=TextAlign.Center)
            Text(credit.person?.biography?.takeIf{it.isNotBlank()}?:"Profile details are not available yet.",Modifier.fillMaxWidth().padding(top=14.dp),fontSize=13.sp,color=Color(0xFFD0D5D9),lineHeight=19.sp,maxLines=6)
            Button(onClick=onDismiss,Modifier.fillMaxWidth().padding(top=16.dp),colors=ButtonDefaults.buttonColors(containerColor=Red)) { Text("Close",color=Color.White) }
        }
    }
}

/** Google Drive share links point to an HTML preview; try its image endpoints before that page. */
private fun imageUrlCandidates(value:String):List<String> {
    val normalized=normalizeWebUrl(value)?:return emptyList()
    return try {
        val uri=Uri.parse(normalized)
        val host=uri.host?.lowercase().orEmpty()
        if(host.endsWith("wikipedia.org")||host.endsWith("wikimedia.org")) return listOf(normalized)
        if(host.endsWith("instagram.com")) {
            val parts=uri.pathSegments
            val kind=parts.firstOrNull()
            if(kind in listOf("p","reel","tv")&&parts.getOrNull(1).isNullOrBlank().not()) {
                val code=parts[1]
                val embed="https://www.instagram.com/$kind/$code/embed/"
                return listOf(normalized,embed)
            }
            return listOf(normalized)
        }
        if(host !in setOf("drive.google.com","www.drive.google.com","docs.google.com")) return listOf(normalized)
        val segments=uri.pathSegments
        val marker=segments.indexOf("d")
        val id=(if(marker>=0) segments.getOrNull(marker+1) else null)
            ?:uri.getQueryParameter("id")
            ?:return listOf(normalized)
        if(id.isBlank()) return listOf(normalized)
        val resourceKey=uri.getQueryParameter("resourcekey")?.let{"&resourcekey=${Uri.encode(it)}"}.orEmpty()
        listOf("https://drive.google.com/thumbnail?id=${Uri.encode(id)}&sz=w1200$resourceKey","https://drive.google.com/uc?export=view&id=${Uri.encode(id)}$resourceKey",normalized)
    } catch(_:Throwable) { listOf(normalized) }
}

private fun imageReferer(source:String):String = when {
    source.contains("wikimedia.org",true)||source.contains("wikipedia.org",true)->"https://en.wikipedia.org/"
    source.contains("googleusercontent.com",true)||source.contains("drive.google.com",true)->"https://drive.google.com/"
    source.contains("cdninstagram.com",true)||source.contains("fbcdn.net",true)->"https://www.instagram.com/"
    else->"https://www.google.com/"
}

@Composable private fun MovieMediaSection(movie:Movie,onPlay:(String,String)->Unit) {
    val media=movie.media.orEmpty()
    val videos=media.filter{it.mediaType in listOf("TRAILER","TRAILER_VERSION","TEASER","BEHIND_THE_SCENES","MAKING_OF","INTERVIEW","EVENT_VIDEO","PROMOTIONAL_VIDEO","OTHER")}
    val photos=media.filterNot{it in videos}
    Column(Modifier.fillMaxWidth().padding(vertical=15.dp)) {
        Text("Trailers & Videos",Modifier.padding(horizontal=16.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
        if(videos.isNotEmpty()) LazyRow(contentPadding=PaddingValues(horizontal=16.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            items(videos.size){i->val item=videos[i];val mediaUrl=item.embedUrl?:item.mediaUrl;Column(Modifier.width(190.dp).clickable{if(!mediaUrl.isNullOrBlank())onPlay(mediaUrl,item.title?:item.mediaType.replace('_',' '))}){Box(Modifier.fillMaxWidth().height(112.dp).background(Panel2,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){val image=item.thumbnailUrl?:movie.backdropUrl?:movie.posterUrl;if(!image.isNullOrBlank())AsyncImage(image,item.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Icon(Icons.Default.PlayCircle,"Play ${item.title?:"video"}",tint=Color.White,modifier=Modifier.size(42.dp))};Text(item.title?:item.mediaType.replace('_',' '),Modifier.padding(top=6.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1);Text(item.language?:if(item.isOfficial)"Official" else "Video",fontSize=10.sp,color=Muted)} }
        } else Text("No videos have been added yet.",Modifier.padding(horizontal=16.dp,vertical=10.dp),color=Muted,fontSize=13.sp)
        Text("Photos & Stills",Modifier.padding(start=16.dp,top=15.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
        val imageUrls=(photos.mapNotNull{it.thumbnailUrl?:it.mediaUrl}+movie.posterImages.orEmpty()+listOfNotNull(movie.posterUrl,movie.backdropUrl)).distinct()
        if(imageUrls.isNotEmpty()) LazyRow(contentPadding=PaddingValues(horizontal=16.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(imageUrls.size){i->Poster(imageUrls[i],Modifier.size(130.dp,155.dp).clip(RoundedCornerShape(9.dp)))}} else Text("No photos or stills have been added yet.",Modifier.padding(horizontal=16.dp,vertical=10.dp),color=Muted,fontSize=13.sp)
    }
}

@Composable private fun MovieReviews(movie:Movie,reviews:List<Review>,error:String,onSeeAll:()->Unit) {
    val rated=reviews.mapNotNull{it.rating}
    val average=if(rated.isNotEmpty()) rated.average() else null
    val ratingScale=if((rated.maxOrNull()?:0)>5) 10 else 5
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text("Audience Reviews",fontSize=18.sp,fontWeight=FontWeight.Bold);Text(movie.title,color=Muted,fontSize=12.sp)};Text("See All  ›",Modifier.clickable{onSeeAll()},color=Red,fontSize=12.sp,fontWeight=FontWeight.SemiBold) }
        if(error.isNotBlank()) Text("Reviews are temporarily unavailable.",Modifier.padding(top=14.dp),color=Muted,fontSize=12.sp)
        else if(reviews.isEmpty()) Text("No published reviews yet. Be the first to share your verdict.",Modifier.padding(top=18.dp),color=Muted,fontSize=13.sp)
        else {
            Row(Modifier.fillMaxWidth().padding(vertical=14.dp).background(Panel,RoundedCornerShape(12.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("?",fontSize=30.sp,color=Color(0xFFFFB51B));Text(average?.let{String.format(java.util.Locale.US,"%.1f",it)}?:"—",Modifier.padding(start=7.dp),fontSize=28.sp,fontWeight=FontWeight.Bold);Text(" / $ratingScale\n(${reviews.size} reviews)",Modifier.padding(start=5.dp),fontSize=11.sp,color=Muted)}
            reviews.take(6).forEach{review->Column(Modifier.fillMaxWidth().padding(vertical=5.dp).background(Panel,RoundedCornerShape(11.dp)).padding(13.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(review.author?:"Audience member",Modifier.weight(1f),fontSize=13.sp,fontWeight=FontWeight.SemiBold);Text("? ${review.rating?:"—"}",color=Color(0xFFFFB51B),fontSize=12.sp)};if(!review.title.isNullOrBlank())Text(review.title,Modifier.padding(top=7.dp),fontSize=13.sp,fontWeight=FontWeight.Medium);Text(review.text.orEmpty(),Modifier.padding(top=5.dp),color=Color(0xFFC2C8CD),fontSize=12.sp,lineHeight=18.sp,maxLines=5);review.createdAt?.take(10)?.let{Text(displayMovieDate(it),Modifier.padding(top=7.dp),fontSize=10.sp,color=Muted)}}}
        }
    }
}

private fun displayMovieDate(value:String):String = try { java.time.LocalDate.parse(value.take(10)).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy",java.util.Locale.getDefault())) } catch(_:Throwable) { value }

/** API dates are calendar dates; ignore any time suffix when comparing them to the selected day. */
private fun showDateKey(value:String):String? = runCatching {
    java.time.LocalDate.parse(value.trim().take(10)).toString()
}.getOrNull()

private fun normalizeCityName(value:String):String = value.lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z]+"),"").let {
    when(it) { "bangalore","bengaluru","bangaloreurban"->"bengaluru";"hyderabad","secunderabad","hyderbad"->"hyderabad";else->it }
}

private fun sameCity(first:String?,second:String?):Boolean = !first.isNullOrBlank()&&!second.isNullOrBlank()&&normalizeCityName(first)==normalizeCityName(second)

private suspend fun resolveDeviceCity(context:Context):String? = withContext(Dispatchers.IO) {
    if(ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return@withContext null
    val manager=context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager?:return@withContext null
    val cached=runCatching { manager.getProviders(true).asSequence().mapNotNull{provider->manager.getLastKnownLocation(provider)}.maxByOrNull{it.time} }.getOrNull()
    val last=cached?.takeIf{System.currentTimeMillis()-it.time<=15*60*1000}
    val location=last?:kotlinx.coroutines.withTimeoutOrNull(12_000) {
        kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { continuation->
            val listener=object:android.location.LocationListener {
                override fun onLocationChanged(location:android.location.Location) {
                    runCatching{manager.removeUpdates(this)}
                    if(continuation.isActive)continuation.resumeWith(Result.success(location))
                }
                override fun onProviderEnabled(provider:String) {}
                override fun onProviderDisabled(provider:String) {}
                @Suppress("DEPRECATION") override fun onStatusChanged(provider:String?,status:Int,extras:android.os.Bundle?) {}
            }
            continuation.invokeOnCancellation{runCatching{manager.removeUpdates(listener)}}
            val provider=runCatching{manager.getProviders(true).firstOrNull{it==android.location.LocationManager.NETWORK_PROVIDER}?:manager.getProviders(true).firstOrNull()}.getOrNull()
            if(provider.isNullOrBlank())continuation.resumeWith(Result.success(null))
            else runCatching{manager.requestSingleUpdate(provider,listener,android.os.Looper.getMainLooper())}.onFailure{if(continuation.isActive)continuation.resumeWith(Result.success(null))}
        }
    }
    if(location==null||!android.location.Geocoder.isPresent())return@withContext null
    @Suppress("DEPRECATION")
    val address=runCatching{android.location.Geocoder(context,java.util.Locale.getDefault()).getFromLocation(location.latitude,location.longitude,1)?.firstOrNull()}.getOrNull()
    address?.locality?.takeIf{it.isNotBlank()}?:address?.subAdminArea?.takeIf{it.isNotBlank()}
}

private fun theatreMatchesCity(theatre:Theatre?,city:String?):Boolean {
    if(theatre==null||city.isNullOrBlank())return false
    if(sameCity(theatre.city,city))return true
    return theatre.address.orEmpty().split(',').any{sameCity(it.trim(),city)}
}

@Composable private fun TrailerPlayerScreen(nav:androidx.navigation.NavHostController,url:String,title:String) {
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    val context=LocalContext.current
    val normalizedUrl=normalizeWebUrl(url)
    val directVideo=normalizedUrl?.let(::isDirectVideoUrl)==true
    val directPlayer=remember(normalizedUrl,directVideo) { if(directVideo) androidx.media3.exoplayer.ExoPlayer.Builder(context).build() else null }
    var isPlaying by remember(normalizedUrl){mutableStateOf(false)}
    var playerError by remember(url){mutableStateOf("")}
    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(directPlayer) {
        val listener=object:androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing:Boolean) { isPlaying=playing }
            override fun onPlayerError(error:androidx.media3.common.PlaybackException) { playerError="This video could not be played. Check that the link is public and uses a supported video format." }
        }
        val lifecycleListener=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_STOP)directPlayer?.pause() }
        directPlayer?.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifecycleListener)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleListener);webView?.stopLoading();webView?.destroy();webView=null;directPlayer?.removeListener(listener);directPlayer?.release() }
    }
    Column(Modifier.fillMaxSize().background(Ink)) {
        PageHeader(title){nav.popBackStack()}
        if(normalizedUrl==null) {
            Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Text("This link is not a valid web address. Use an http or https link.",Modifier.padding(24.dp),color=Color.White,textAlign=TextAlign.Center)}
        } else {
            val youtubeId=extractYoutubeId(normalizedUrl)
            val drivePreview=googleDrivePreviewUrl(normalizedUrl)
            val html=when {
                youtubeId!=null -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><style>html,body{margin:0;width:100%;height:100%;background:#000;overflow:hidden}iframe{border:0;width:100%;height:100%}</style></head><body><iframe src="https://www.youtube-nocookie.com/embed/$youtubeId?autoplay=1&playsinline=1&rel=0&origin=https%3A%2F%2Fwww.youtube-nocookie.com" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe></body></html>"""
                drivePreview!=null -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><style>html,body{margin:0;width:100%;height:100%;background:#000;overflow:hidden}iframe{border:0;width:100%;height:100%}</style></head><body><iframe src="${android.text.TextUtils.htmlEncode(drivePreview)}" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe></body></html>"""
                directVideo -> {
                    val safeUrl=android.text.TextUtils.htmlEncode(normalizedUrl)
                """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><style>html,body{margin:0;width:100%;height:100%;background:#000}video{width:100%;height:100%;object-fit:contain;background:#000}</style></head><body><video controls autoplay playsinline><source src="$safeUrl"></video></body></html>"""
                }
                else -> null
            }
            if(directVideo) {
                val player=directPlayer
                LaunchedEffect(player,normalizedUrl) {
                    if(player!=null) {
                        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(normalizedUrl))
                        player.prepare()
                        player.playWhenReady=true
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black),contentAlignment=Alignment.Center) {
                    if(player!=null) AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->
                        android.widget.FrameLayout(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            val texture=android.view.TextureView(ctx)
                            addView(texture,android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT))
                            player.setVideoTextureView(texture)
                        }
                    })
                    if(playerError.isNotBlank()) Text(playerError,Modifier.align(Alignment.Center).background(Color(0xDD15191C),RoundedCornerShape(12.dp)).padding(16.dp),color=Color.White,textAlign=TextAlign.Center,fontSize=13.sp)
                    IconButton(onClick={player?.let{if(it.isPlaying)it.pause() else it.play()}},modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=20.dp).size(54.dp).background(Color(0xCC17191C),CircleShape)) {
                        Icon(if(isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,if(isPlaying)"Pause trailer" else "Play trailer",tint=Color.White,modifier=Modifier.size(30.dp))
                    }
                }
            } else {
                AndroidView(modifier=Modifier.fillMaxWidth().weight(1f).background(Color.Black),factory={ctx->
                    android.webkit.WebView(ctx).apply {
                        webView=this
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE,null)
                        settings.javaScriptEnabled=true
                        settings.domStorageEnabled=true
                        settings.mediaPlaybackRequiresUserGesture=false
                        webChromeClient=android.webkit.WebChromeClient()
                        webViewClient=android.webkit.WebViewClient()
                        settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        if(html!=null) loadDataWithBaseURL(if(youtubeId!=null) "https://www.youtube-nocookie.com/" else "https://drive.google.com/",html,"text/html","UTF-8",null)
                        else loadUrl(normalizedUrl)
                    }
                })
            }
            if(playerError.isNotBlank()) Text(playerError,Modifier.fillMaxWidth().background(Color(0xFF321115)).padding(12.dp),color=Color.White,fontSize=12.sp)
            Text(if(youtubeId!=null) "YouTube player · inside Audience Verdict" else if(drivePreview!=null) "Google Drive player · inside Audience Verdict" else "Opened inside Audience Verdict",Modifier.fillMaxWidth().padding(14.dp),color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
        }
    }
}

private fun extractYoutubeId(value:String):String? {
    return try {
        val normalized=normalizeWebUrl(value)?:return null
        val uri=Uri.parse(normalized)
        val host=uri.host?.lowercase().orEmpty()
        val candidate=when {
            host.endsWith("youtu.be") -> uri.lastPathSegment
            host.contains("youtube.com")||host.contains("youtube-nocookie.com") -> uri.getQueryParameter("v")?:run {
                val parts=uri.pathSegments
                val marker=parts.indexOfFirst{it in listOf("embed","shorts","live")}
                if(marker>=0) parts.getOrNull(marker+1) else null
            }
            else -> null
        }
        candidate?.takeIf{it.matches(Regex("[A-Za-z0-9_-]{6,}"))}
    } catch(_:Throwable) { null }
}

private fun normalizeWebUrl(value:String):String? {
    return try {
        val raw=value.trim().removeSurrounding("\"").removeSurrounding("'").trim('<','>')
        if(raw.isBlank()) return null
        val candidate=if(Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(raw)) raw else "https://$raw"
        val uri=Uri.parse(candidate)
        if(uri.scheme?.lowercase() in listOf("http","https")&&!uri.host.isNullOrBlank()) uri.toString() else null
    } catch(_:Throwable) { null }
}

private fun googleDrivePreviewUrl(value:String):String? {
    return try {
        val normalized=normalizeWebUrl(value)?:return null
        val uri=Uri.parse(normalized)
        val host=uri.host?.lowercase().orEmpty()
        if(!host.contains("drive.google.com")&&!host.contains("docs.google.com")) return null
        val parts=uri.pathSegments
        val index=parts.indexOf("d")
        val id=if(index>=0) parts.getOrNull(index+1) else uri.getQueryParameter("id")
        id?.takeIf{it.isNotBlank()}?.let{"https://drive.google.com/file/d/${Uri.encode(it)}/preview"}
    } catch(_:Throwable) { null }
}

private fun isDirectVideoUrl(value:String):Boolean = try {
    val uri=Uri.parse(value)
    val path=uri.path?.lowercase().orEmpty()
    val type=uri.getQueryParameter("mimeType")?.lowercase().orEmpty()
    listOf(".mp4",".m4v",".webm",".ogv",".mov",".m3u8",".mpd").any{path.endsWith(it)} || type.startsWith("video/")
} catch(_:Throwable) { false }

@Composable private fun PageHeader(title:String,onBack:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(start=4.dp,end=16.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back",tint=Color.White)}
        Text(title,Modifier.padding(start=4.dp),fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1)
    }
}

data class SeatStateDto(val id:String="",val label:String="",val rowNumber:Int=0,val columnNumber:Int=0,val category:String="",val color:String?=null,val disabled:Boolean=false,val state:String="AVAILABLE",val clickable:Boolean=true)
data class ShowLayoutDto(val seats:List<SeatStateDto> = emptyList())
data class AvailabilityDto(val show:Show?=null,val layout:ShowLayoutDto?=null,val bookedSeatIds:List<String> = emptyList(),val reservedSeatIds:List<String> = emptyList(),val heldSeatIds:List<String> = emptyList(),val availableSeatCount:Int=0)
data class SeatIdsPayload(val showId:String,val seatIds:List<String>)
data class BookingPayload(val showId:String,val seatIds:List<String>,val termsAccepted:Boolean=true,val alternativeMobile:String?=null)
data class CancelBookingPayload(val seatIds:List<String>)

private fun parseAvailability(json:com.google.gson.JsonElement):AvailabilityDto {
    val gson=com.google.gson.Gson()
    if(json.isJsonObject) return gson.fromJson(json,AvailabilityDto::class.java)
    if(json.isJsonArray) {
        val values=json.asJsonArray
        val wrapper=values.firstOrNull{it.isJsonObject&&(it.asJsonObject.has("show")||it.asJsonObject.has("layout"))}
        if(wrapper!=null) return gson.fromJson(wrapper,AvailabilityDto::class.java)
        val seats=values.flatMap { value ->
            when {
                value.isJsonObject -> listOfNotNull(runCatching{gson.fromJson(value,SeatStateDto::class.java)}.getOrNull())
                value.isJsonArray -> value.asJsonArray.mapNotNull{seat->if(seat.isJsonObject)runCatching{gson.fromJson(seat,SeatStateDto::class.java)}.getOrNull() else null}
                else -> emptyList()
            }
        }
        return AvailabilityDto(layout=ShowLayoutDto(seats=seats))
    }
    throw IllegalStateException("Seat availability returned an unsupported JSON response.")
}

private suspend fun loadSeatLabels(showId:String,seatIds:List<String>):List<String> {
    if(showId.isBlank()||seatIds.isEmpty()) return emptyList()
    val seats=parseAvailability(AvClient.api.availability(showId)).layout?.seats.orEmpty()
    val labels=seats.associate{it.id to it.label}
    return seatIds.mapNotNull{id->labels[id]?.takeIf{it.isNotBlank()}}
}

private fun seatHoldExpiry(json:com.google.gson.JsonElement):String {
    val expiries=when {
        json.isJsonArray -> json.asJsonArray.mapNotNull { item -> item.takeIf{it.isJsonObject}?.asJsonObject?.get("expiresAt")?.takeUnless{it.isJsonNull}?.asString }
        json.isJsonObject -> listOfNotNull(json.asJsonObject.get("expiresAt")?.takeUnless{it.isJsonNull}?.asString)
        else -> emptyList()
    }
    return expiries.minOrNull().orEmpty()
}

private fun holdExpirySeconds(value:String):Long = try {
    java.time.Duration.between(java.time.Instant.now(),java.time.Instant.parse(value)).seconds.coerceAtLeast(0L)
} catch(_:Throwable) { 0L }

private fun activeWaitingEntry(entries:List<WaitingListEntry>,showId:String?=null):WaitingListEntry? {
    val matching=entries.filter{(showId==null||it.showId==showId)&&it.status.uppercase() in setOf("WAITING","OFFERED")}
    return matching.firstOrNull{it.status.equals("OFFERED",true)} ?: matching.firstOrNull()
}

private fun waitingOfferExpired(entry:WaitingListEntry,now:java.time.Instant=java.time.Instant.now()):Boolean {
    val expires=entry.offerExpiresAt?.let{runCatching{java.time.Instant.parse(it)}.getOrNull()} ?: return false
    return !expires.isAfter(now)
}

private fun displayTimestamp(value:String?):String? = value?.let { raw ->
    runCatching {
        java.time.OffsetDateTime.parse(raw).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, h:mm a",java.util.Locale.getDefault()))
    }.getOrElse { raw.replace('T',' ').take(16) }
}

private fun makeTicketPdf(context:Context,booking:BookingRecord,seatLabels:List<String>,movie:Movie?,show:Show?,theatre:Theatre?,qr:Bitmap?,poster:Bitmap?):ByteArray {
    val document=android.graphics.pdf.PdfDocument()
    val page=document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create())
    val canvas=page.canvas
    val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(android.graphics.Color.rgb(7,10,12))
    paint.color=android.graphics.Color.rgb(255,52,59);canvas.drawRect(0f,0f,595f,18f,paint)
    val logoDrawable=context.getDrawable(R.drawable.av_logo)
    if(logoDrawable!=null){val logo=Bitmap.createBitmap(270,100,Bitmap.Config.ARGB_8888);val lc=android.graphics.Canvas(logo);logoDrawable.setBounds(0,0,270,100);logoDrawable.draw(lc);canvas.drawBitmap(logo,null,android.graphics.RectF(35f,34f,260f,117f),paint)}
    paint.color=android.graphics.Color.WHITE;paint.textSize=26f;paint.typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD);canvas.drawText("MOVIE TICKET",35f,158f,paint)
    paint.color=android.graphics.Color.rgb(39,213,128);paint.textSize=15f;canvas.drawText("${booking.status.uppercase()}  ·  AUDIENCE VERDICT",35f,184f,paint)
    paint.color=android.graphics.Color.rgb(28,34,38);canvas.drawRoundRect(28f,208f,567f,497f,20f,20f,paint)
    if(poster!=null)canvas.drawBitmap(poster,null,android.graphics.RectF(45f,226f,190f,430f),paint)
    paint.color=android.graphics.Color.WHITE;paint.textSize=21f;canvas.drawText((movie?.title?:booking.movie?:"Movie").take(34),212f,250f,paint)
    paint.textSize=14f;paint.color=android.graphics.Color.LTGRAY
    val details=listOfNotNull(movie?.language?:movie?.originalLanguage,(movie?.durationMinutes?:movie?.duration)?.let{"$it min"},theatre?.name?:booking.theatre,show?.date?:booking.date,show?.startTime?:booking.time,booking.screen?.let{"Screen $it"},"Seats: ${seatLabels.joinToString(", ").ifBlank{"${booking.ticketCount} ticket(s)"}}")
    details.take(7).forEachIndexed{i,line->canvas.drawText(line.take(46),212f,282f+i*24f,paint)}
    paint.color=android.graphics.Color.rgb(255,52,59);paint.textSize=13f;paint.typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD);canvas.drawText("BOOKING REFERENCE",45f,467f,paint)
    paint.color=android.graphics.Color.WHITE;paint.textSize=16f;canvas.drawText(booking.id.take(37),45f,487f,paint)
    paint.color=android.graphics.Color.rgb(27,32,36);canvas.drawRoundRect(28f,518f,567f,788f,18f,18f,paint)
    if(qr!=null){paint.color=android.graphics.Color.WHITE;canvas.drawRoundRect(44f,535f,290f,781f,12f,12f,paint);canvas.drawBitmap(qr,null,android.graphics.RectF(54f,545f,280f,771f),paint)}
    paint.color=android.graphics.Color.WHITE;paint.textSize=17f;paint.typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD);canvas.drawText("Present at cinema entry",315f,570f,paint)
    paint.textSize=12f;paint.typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL);paint.color=android.graphics.Color.LTGRAY
    listOf("Scan the backend issued ticket QR.","Ticket code: ${booking.ticketCode?:"Pending"}","Amount: ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en","IN")).format(booking.totalAmount)}","Keep this ticket available at entry.").forEachIndexed{i,line->canvas.drawText(line.take(36),315f,603f+i*25f,paint)}
    paint.color=android.graphics.Color.rgb(255,52,59);canvas.drawRect(28f,812f,567f,815f,paint)
    paint.color=android.graphics.Color.LTGRAY;paint.textSize=10f;canvas.drawText("Audience Verdict  ·  We show. You decide.",35f,833f,paint)
    document.finishPage(page)
    val output=java.io.ByteArrayOutputStream();document.writeTo(output);document.close();return output.toByteArray()
}

private inline fun <reified T> parseApiList(json:com.google.gson.JsonElement):List<T> {
    val gson=com.google.gson.Gson()
    val array=when {
        json.isJsonArray->json.asJsonArray
        json.isJsonObject->listOf("content","items","entries","notifications","waitingList","data").firstNotNullOfOrNull{key->json.asJsonObject.get(key)?.takeIf{it.isJsonArray}?.asJsonArray}
        else->null
    }?:return emptyList()
    return array.mapNotNull{item->runCatching{gson.fromJson(item,T::class.java)}.getOrNull()}
}

private fun postNewLocalNotifications(context:Context,items:List<AvNotification>) {
    if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
    val latest=items.firstOrNull{!it.id.isNullOrBlank()}?:return
    val prefs=context.getSharedPreferences("av_notification_state",Context.MODE_PRIVATE)
    val previous=prefs.getString("latest_id",null)
    if(previous==null){prefs.edit().putString("latest_id",latest.id).apply();return}
    val fresh=items.takeWhile{it.id!=previous}.filter{!it.id.isNullOrBlank()}
    if(fresh.isEmpty())return
    val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    if(android.os.Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(android.app.NotificationChannel("av_updates","Audience Verdict updates",android.app.NotificationManager.IMPORTANCE_DEFAULT))
    fresh.reversed().forEach{item->
        val type=item.type.orEmpty().uppercase()
        val content=item.message?:item.body.orEmpty()
        val isBooking=type.contains("BOOKING")||type.contains("TICKET")
        val isMovie=type.contains("MOVIE")||content.contains("new movie",true)||content.contains("now open for booking",true)
        val destination=if(isBooking)"bookings" else if(isMovie)"movie" else "notifications"
        val rawTitle=item.movieTitle?:content.ifBlank{item.title.orEmpty()}
        val cleanedTitle=rawTitle.replace(Regex("(?i)^(a\\s+)?(new movie(?: added)?|movie added|now open for booking)\\s*[:—-]?\\s*"),"").replace(Regex("(?i)\\s+(is now open for booking|is available for booking|added to audience verdict)[.!]*$"),"").trim()
        val tapIntent=Intent(context,MainActivity::class.java).apply {
            flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("av_notification_destination",destination)
            putExtra("av_movie_id",item.movieId?:item.targetId.orEmpty())
            putExtra("av_movie_title",cleanedTitle)
        }
        val tapPendingIntent=android.app.PendingIntent.getActivity(context,item.id.hashCode(),tapIntent,android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val notification=androidx.core.app.NotificationCompat.Builder(context,"av_updates")
            .setSmallIcon(R.drawable.av_notification)
            .setContentTitle(item.title?:"Audience Verdict update")
            .setContentText(content.ifBlank{"You have a new update."})
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(content))
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources,R.drawable.av_logo))
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true).build()
        manager.notify(item.id.hashCode(),notification)
    }
    prefs.edit().putString("latest_id",latest.id).apply()
}

@Composable private fun NotificationMovieRoute(nav:androidx.navigation.NavHostController,title:String) {
    var error by remember(title){mutableStateOf("")}
    val context=LocalContext.current
    LaunchedEffect(title) {
        try {
            val candidates=AvClient.api.movies(title=title.take(100)).content
            val normalized=title.lowercase().replace(Regex("[^a-z0-9]+")," ").trim()
            val match=candidates.firstOrNull{movie->movie.id.isNotBlank()&&movie.title.lowercase().replace(Regex("[^a-z0-9]+")," ").trim()==normalized}
                ?:candidates.firstOrNull{movie->movie.id.isNotBlank()&&(normalized.contains(movie.title.lowercase())||movie.title.lowercase().contains(normalized))}
            if(match!=null)nav.navigate("details/${Uri.encode(match.id)}"){popUpTo("notification-movie/{title}"){inclusive=true};launchSingleTop=true}
            else nav.navigate("movies"){popUpTo("notification-movie/{title}"){inclusive=true};launchSingleTop=true}
        } catch(t:Throwable) { error=apiErrorMessage(t,context) }
    }
    Column(Modifier.fillMaxSize().background(Ink),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        if(error.isBlank())Busy() else {ErrorCard(error);TextButton(onClick={nav.navigate("movies"){popUpTo("notification-movie/{title}"){inclusive=true}}}){Text("Browse movies",color=Red)}}
    }
}


@Composable private fun ShowList(nav:androidx.navigation.NavHostController,movieId:String) {
    val context=LocalContext.current
    val lifecycleOwner=LocalLifecycleOwner.current
    val locationScope= rememberCoroutineScope()
    var shows by remember{mutableStateOf<List<Show>>(emptyList())}
    var theatres by remember{mutableStateOf<List<Theatre>>(emptyList())}
    var error by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(false)}
    var userCity by remember{mutableStateOf<String?>(null)}
    var locationChecked by remember{mutableStateOf(false)}
    var locationEnabled by remember{mutableStateOf(false)}
    var showLocationDialog by remember{mutableStateOf(false)}
    var cityMode by remember{mutableStateOf("LIVE")}
    var date by remember{mutableStateOf(java.time.LocalDate.now().toString())}
    val refreshLocation:suspend ()->Unit={
        locationChecked=false
        val manager=context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        locationEnabled=runCatching{manager?.isLocationEnabled==true}.getOrDefault(false)
        userCity=runCatching{resolveDeviceCity(context)}.getOrNull()
        locationChecked=true
    }
    LaunchedEffect(Unit){refreshLocation()}
    DisposableEffect(lifecycleOwner) {
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)locationScope.launch{refreshLocation()}}
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(movieId,date) {
        loading=true;error="";shows=emptyList()
        try {
            val requestedDate=date
            val response=AvClient.api.shows(movieId=movieId,date=requestedDate)
            // Some API responses include neighboring dates even with the date query. Never
            // render those under the selected day; show.date is defined as YYYY-MM-DD.
            shows=response.filter { showDateKey(it.date)==requestedDate }
            theatres=AvClient.api.theatres().content
        }
        catch(t:Throwable) { error=apiErrorMessage(t,context) }
        finally { loading=false }
    }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.showtimes_cinema_bg),"",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x9905080B),Color(0xC9080A0B),Color(0xEA080A0B)))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp)) {
            PageHeader("Select Showtimes"){nav.popBackStack()}
            Text("Choose a date",Modifier.padding(start=3.dp,top=12.dp),fontSize=17.sp,color=Color(0xFFD4D8DC))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                (0..5).map{java.time.LocalDate.now().plusDays(it.toLong())}.forEach{day->
                    val selected=date==day.toString()
                    Column(Modifier.width(61.dp).height(76.dp).background(if(selected)Red else Color(0xB3080A0B),RoundedCornerShape(14.dp)).then(if(selected)Modifier else Modifier.border(1.dp,Color(0xFF626970),RoundedCornerShape(14.dp))).clickable{date=day.toString()},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        Text(day.dayOfWeek.name.take(3),fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
                        Text(day.dayOfMonth.toString(),Modifier.padding(top=2.dp),fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    }
                }
            }
            when {
                loading -> Busy()
                error.isNotBlank() -> ErrorCard(error)
                !locationChecked -> Text("Checking your live city…",Modifier.fillMaxWidth().padding(20.dp),fontSize=13.sp,color=Muted,textAlign=TextAlign.Center)
                shows.isEmpty() -> Text("No available showtimes for this date.",Modifier.padding(16.dp),color=Muted)
                else -> {
                Row(Modifier.fillMaxWidth().padding(top=5.dp,bottom=8.dp).background(Panel,RoundedCornerShape(14.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("LIVE" to "Live city${userCity?.let{" · $it"}.orEmpty()}","OTHER" to "Other cities").forEach{(mode,label)->
                        val selected=cityMode==mode
                        Box(Modifier.weight(1f).background(if(selected)Red else Color.Transparent,RoundedCornerShape(10.dp)).clickable{cityMode=mode;if(mode=="LIVE"&&(userCity.isNullOrBlank()||!locationEnabled))showLocationDialog=true}.padding(horizontal=6.dp,vertical=11.dp),contentAlignment=Alignment.Center){Text(label,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=if(selected)Color.White else Muted,maxLines=1)}
                    }
                }
                val localShows=shows.filter{show->theatreMatchesCity(theatres.firstOrNull{it.id==show.theatreId},userCity)}
                val otherShows=shows.filterNot{show->theatreMatchesCity(theatres.firstOrNull{it.id==show.theatreId},userCity)}
                val visibleShows=if(cityMode=="LIVE")localShows else otherShows
                if(cityMode=="LIVE"&&(userCity.isNullOrBlank()||!locationEnabled)) Column(Modifier.fillMaxWidth().padding(top=8.dp,bottom=8.dp).border(1.dp,Red.copy(alpha=.8f),RoundedCornerShape(14.dp)).background(Color(0xFF211114),RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(if(!locationEnabled)"Turn on location to book tickets" else "Couldn't determine your live city",fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Text(if(!locationEnabled)"Turn on your phone's location service and allow location access. Ticket booking is disabled until we can verify that the cinema is in your city." else "Allow location access and try again. Ticket booking stays disabled until we can verify the cinema is in your city.",Modifier.padding(top=5.dp),fontSize=12.sp,lineHeight=17.sp,color=Color(0xFFD0D5D9))
                    Button(onClick={if(!locationEnabled)runCatching{context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))}else locationScope.launch{refreshLocation()}},Modifier.fillMaxWidth().padding(top=10.dp),colors=ButtonDefaults.buttonColors(containerColor=Red),shape=RoundedCornerShape(10.dp)) { Text(if(!locationEnabled)"Open location settings" else "Check location again",fontWeight=FontWeight.Bold) }
                }
                else if(visibleShows.isEmpty()) Text(if(cityMode=="LIVE")"No showtimes in $userCity for this date." else "No showtimes in other cities for this date.",Modifier.fillMaxWidth().padding(16.dp),fontSize=13.sp,color=Muted,textAlign=TextAlign.Center)
                else if(cityMode=="OTHER") Text("Other city shows are for browsing only. Booking is disabled because your live location is in ${userCity ?: "an unverified city"}.",Modifier.fillMaxWidth().padding(top=6.dp,bottom=4.dp).background(Color(0xFF261317),RoundedCornerShape(10.dp)).padding(11.dp),fontSize=12.sp,lineHeight=16.sp,color=Color(0xFFFFA1A8))
                visibleShows.groupBy{it.theatreId}.forEach{(theatreId,theatreShows)->
                    val theatre=theatres.firstOrNull{it.id==theatreId}
                    val theatreCity=theatre?.city?.takeIf{it.isNotBlank()} ?: theatre?.address.orEmpty().split(',').lastOrNull()?.trim().orEmpty()
                    Column(Modifier.fillMaxWidth().padding(top=10.dp,bottom=8.dp).border(1.dp,Color(0xFF8F2029),RoundedCornerShape(21.dp)).background(Brush.linearGradient(listOf(Color(0xF0181114),Color(0xE9080A0B))),RoundedCornerShape(21.dp)).padding(15.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.seat_cinema_bg),"Cinema auditorium",Modifier.size(82.dp,76.dp).background(Panel2,RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start=13.dp)) {
                                Text(theatre?.name?.takeIf{it.isNotBlank()}?:"Cinema",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color.White)
                                Row(Modifier.padding(top=5.dp),verticalAlignment=Alignment.Top) {
                                    Icon(Icons.Default.LocationOn,"Location",Modifier.size(17.dp),tint=Red)
                                Text(listOfNotNull(theatre?.address,theatre?.city).joinToString(", ").ifBlank{"Location details unavailable"},Modifier.padding(start=5.dp),fontSize=11.sp,color=Color(0xFFBFC5CA),lineHeight=16.sp,maxLines=3)
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical=13.dp),color=Color(0xFF343A40))
                        Text("AVAILABLE SHOWTIMES",fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.SemiBold,color=Muted)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                            theatreShows.forEach{show->
                                val price=if(show.ticketType.equals("FREE",true))"FREE" else if(show.ticketPrice>0)java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en","IN")).format(show.ticketPrice) else show.ticketType.ifBlank{"SELECT"}
                                Column(Modifier.widthIn(min=128.dp).border(1.dp,if(cityMode=="LIVE"&&userCity!=null&&locationEnabled)Red.copy(alpha=.8f) else Muted.copy(alpha=.5f),RoundedCornerShape(14.dp)).background(Color(0x66120A0C),RoundedCornerShape(14.dp)).clickable(enabled=cityMode=="LIVE"&&userCity!=null&&locationEnabled){nav.navigate("seats/${show.id}")}.padding(horizontal=14.dp,vertical=11.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AccessTime,"Showtime",Modifier.size(17.dp),tint=Red);Text(show.startTime,Modifier.padding(start=7.dp),fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)}
                                    Text(price,Modifier.padding(top=5.dp).background(if(price=="FREE")Color(0x3320C77A) else Panel2,RoundedCornerShape(20.dp)).padding(horizontal=11.dp,vertical=4.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,color=if(price=="FREE")Color(0xFF31D27E) else Color(0xFFD8DCE0))
                                    if(cityMode=="OTHER") Text("Booking unavailable · $theatreCity",Modifier.padding(top=5.dp),fontSize=9.sp,color=Muted,maxLines=1)
                                    else if(!locationEnabled||userCity.isNullOrBlank()) Text("Turn on location to book",Modifier.padding(top=5.dp),fontSize=9.sp,color=Color(0xFFFFA1A8),maxLines=1)
                                }
                            }
                        }
                    }
                }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if(showLocationDialog) AlertDialog(
        onDismissRequest={showLocationDialog=false},
        title={Text("Turn on location to book",color=Color.White,fontWeight=FontWeight.Bold)},
        text={Text("Audience Verdict needs your live city to confirm that this show is in your city. You can browse other cities, but booking stays disabled until location is on and verified.",color=Color(0xFFD0D5D9))},
        confirmButton={TextButton(onClick={showLocationDialog=false;runCatching{context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))}}){Text("Open location settings",color=Red)}},
        dismissButton={TextButton(onClick={showLocationDialog=false}){Text("Not now",color=Color.White)}},
        containerColor=Panel
    )
}

@Composable private fun SeatInfo(nav:androidx.navigation.NavHostController,showId:String) {
    var data by remember{mutableStateOf<AvailabilityDto?>(null)}
    var selected by remember{mutableStateOf(setOf<String>())}
    var msg by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var priorShowTickets by remember(showId){mutableIntStateOf(0)}
    var waitingEntries by remember(showId){mutableStateOf<List<WaitingListEntry>>(emptyList())}
    var waitlistSeats by remember(showId){mutableIntStateOf(1)}
    val scope= rememberCoroutineScope()
    LaunchedEffect(showId) { try{val availability=parseAvailability(AvClient.api.availability(showId));data=if(availability.show==null)availability.copy(show=AvClient.api.show(showId)) else availability}catch(t:Throwable){msg=apiErrorMessage(t)} }
    LaunchedEffect(showId) { try { priorShowTickets=AvClient.api.bookings().filter{it.showId==showId&&it.status.uppercase() !in setOf("CANCELLED","EXPIRED","REJECTED")}.sumOf{it.ticketCount} } catch(_:Throwable) { } }
    LaunchedEffect(showId) { try { waitingEntries=AvClient.api.waitingList().filter{it.showId==showId} } catch(_:Throwable) { } }
    LaunchedEffect(showId) { while(isActive) { delay(5000); try { data=parseAvailability(AvClient.api.availability(showId)) } catch(_:Throwable) { };try{waitingEntries=AvClient.api.waitingList().filter{it.showId==showId}}catch(_:Throwable){} } }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.seat_cinema_bg),"",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0xB9080A0B)))
        Column(Modifier.fillMaxSize().padding(horizontal=14.dp)) {
            PageHeader("Select Seats"){nav.popBackStack()}
            Box(Modifier.fillMaxWidth().height(104.dp).padding(top=5.dp,bottom=8.dp).border(1.dp,Red.copy(alpha=.85f),RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(0xAA4D1015),Color(0xA8050709))),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) {
                Text("SCREEN THIS WAY",fontSize=12.sp,letterSpacing=3.sp,fontWeight=FontWeight.Medium,color=Color(0xFFF1F1F2))
            }
            Spacer(Modifier.height(7.dp))
            val d=data
            if(d==null) {
                if(msg.isNotBlank()) ErrorCard(msg) else Busy()
            } else {
                val seats=d.layout?.seats.orEmpty()
                val availableSeats=seats.count{seat->!seat.disabled&&seat.clickable&&seat.state.uppercase() !in listOf("BOOKED","HELD","RESERVED","UNAVAILABLE")&&seat.id !in d.bookedSeatIds&&seat.id !in d.heldSeatIds&&seat.id !in d.reservedSeatIds}
                val ticketLimit=d.show?.maxTicketsPerMobile?.takeIf{it>0}
                val remainingTickets=ticketLimit?.let{(it-priorShowTickets).coerceAtLeast(0)}
                val selectionLimit=remainingTickets?:ticketLimit?:Int.MAX_VALUE
                LaunchedEffect(remainingTickets,seats.size){val maxRequest=(remainingTickets?:seats.size).coerceIn(0,seats.size);waitlistSeats=if(maxRequest>0)waitlistSeats.coerceIn(1,maxRequest) else 1}
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally) {
                    if(seats.isEmpty()) Text("Seat layout has not been published for this show yet.",Modifier.padding(26.dp),color=Color(0xFFD0D5D9),textAlign=TextAlign.Center)
                    else {
                        Spacer(Modifier.height(12.dp))
                        seats.groupBy{it.rowNumber}.toSortedMap().forEach{(_,seatRow)->
                            val row=seatRow.sortedBy{it.columnNumber}
                            val rowName=row.firstOrNull()?.label?.takeWhile{it.isLetter()}?.ifBlank{null}?:"${row.firstOrNull()?.rowNumber?:""}"
                            Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                                Text(rowName,Modifier.width(22.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFFD6DADF),textAlign=TextAlign.Center)
                                row.forEachIndexed{index,seat->
                                    if(row.size>=9&&index==row.size/2) Spacer(Modifier.width(14.dp))
                                    val blocked=seat.disabled||!seat.clickable||seat.state.uppercase() in listOf("BOOKED","HELD","RESERVED","UNAVAILABLE")||seat.id in d.bookedSeatIds||seat.id in d.heldSeatIds||seat.id in d.reservedSeatIds
                                    val chosen=seat.id in selected
                                    val shape=RoundedCornerShape(topStart=8.dp,topEnd=8.dp,bottomStart=5.dp,bottomEnd=5.dp)
                                    val premium=seat.category.uppercase() in listOf("PREMIUM","GOLD","PLATINUM","RECLINER","VIP")
                                    val seatSize=when{row.size>=12->19.dp;row.size>=10->22.dp;else->25.dp}
                                    Box(Modifier.padding(horizontal=1.dp).size(seatSize,seatSize).then(if(chosen)Modifier.border(1.5.dp,Color(0xFFFF5961),shape) else if(!blocked&&premium)Modifier.border(1.5.dp,Color(0xFFFFC45C),shape) else if(!blocked)Modifier.border(1.dp,Color(0xFFC5CBD1),shape) else Modifier).background(when{chosen->Red;blocked->Color(0xFF34383D);premium->Color(0xFF60471F);else->Color(0x332B3035)},shape).clickable(enabled=!blocked&&(chosen||selected.size<selectionLimit)){selected=if(chosen)selected-seat.id else selected+seat.id},contentAlignment=Alignment.Center) {
                                        Text(seat.label.takeLastWhile{it.isDigit()},fontSize=7.sp,color=if(blocked||chosen)Color.White else Color(0xFFD9DDE0))
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top=14.dp,bottom=8.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                        SeatLegend(Color(0xFFBBC2C8),"Available")
                        Spacer(Modifier.width(13.dp));SeatLegend(Red,"Selected")
                        Spacer(Modifier.width(13.dp));SeatLegend(Color(0xFF42474D),"Unavailable")
                    }
                    if(seats.any{it.category.uppercase() in listOf("PREMIUM","GOLD","PLATINUM","RECLINER","VIP")}) Row(Modifier.fillMaxWidth().padding(bottom=5.dp),horizontalArrangement=Arrangement.Center){SeatLegend(Color(0xFFFFC45C),"Premium")}
                }
                val selectedSeats=seats.filter{it.id in selected}
                val total=if(d.show?.ticketType.equals("FREE",true))0.0 else d.show?.ticketPrice?.takeIf{it>0}?.times(selected.size)
                Row(Modifier.fillMaxWidth().padding(top=4.dp).border(1.dp,Red.copy(alpha=.85f),RoundedCornerShape(18.dp)).background(Color(0xD90A0C0E),RoundedCornerShape(18.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${selected.size} ${if(selected.size==1)"seat" else "seats"} selected",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)
                        if(selectedSeats.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=7.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            selectedSeats.forEach{seat->Text(seat.label,Modifier.background(Color(0x442A080C),RoundedCornerShape(18.dp)).clickable{selected=selected-seat.id}.padding(horizontal=11.dp,vertical=6.dp),fontSize=12.sp,color=Color(0xFFFFD7D9))}
                        }
                    }
                    Column(Modifier.padding(start=12.dp),horizontalAlignment=Alignment.End) {
                        Text("Total Amount",fontSize=11.sp,color=Muted)
                        Text(if(total==null)"—" else java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en","IN")).format(total),Modifier.padding(top=4.dp),fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    }
                }
                if(ticketLimit!=null) Text("Limit: $ticketLimit tickets per mobile for this show · ${remainingTickets?:ticketLimit} remaining",Modifier.fillMaxWidth().padding(top=5.dp),fontSize=10.sp,color=if(remainingTickets==0)Red else Muted,textAlign=TextAlign.Center)
                if(msg.isNotBlank()) Text(msg,Modifier.fillMaxWidth().padding(top=6.dp),color=Red,fontSize=11.sp,maxLines=2)
                Button(onClick={scope.launch{busy=true;msg="";try{val held=AvClient.api.hold(SeatIdsPayload(showId,selected.toList()));val expires=seatHoldExpiry(held);nav.navigate("terms/${Uri.encode(showId)}?seats=${Uri.encode(selected.joinToString(","))}&expires=${Uri.encode(expires)}")}catch(t:Throwable){msg=apiErrorMessage(t);try{data=parseAvailability(AvClient.api.availability(showId))}catch(_:Throwable){}}finally{busy=false}}},Modifier.fillMaxWidth().padding(top=10.dp,bottom=12.dp).height(54.dp),enabled=selected.isNotEmpty()&&selected.size<=selectionLimit&&!busy,colors=ButtonDefaults.buttonColors(containerColor=Red,disabledContainerColor=Color(0xFF67262B)),shape=RoundedCornerShape(28.dp)) {
                    Text(if(busy)"Holding seats…" else "Continue · ${selected.size} seats     >",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)
                }
                if(availableSeats==0&&seats.isNotEmpty()) {
                    val currentEntry=activeWaitingEntry(waitingEntries,showId)
                    val waitlistLimit=minOf(selectionLimit,seats.size).coerceAtLeast(0)
                    if(currentEntry==null) {
                        if(waitlistLimit==0) {
                            Text("You have reached the per-mobile ticket limit for this show, so you cannot join its waiting list.",Modifier.fillMaxWidth().padding(vertical=10.dp),fontSize=11.sp,color=Muted,textAlign=TextAlign.Center)
                        } else {
                            Row(Modifier.fillMaxWidth().padding(top=9.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                                Text("Tickets requested",fontSize=12.sp,color=Muted)
                                Spacer(Modifier.width(15.dp))
                                OutlinedButton(onClick={waitlistSeats=(waitlistSeats-1).coerceAtLeast(1)},enabled=waitlistSeats>1,contentPadding=PaddingValues(horizontal=13.dp,vertical=3.dp)){Text("−",fontSize=18.sp,color=Color.White)}
                                Text("$waitlistSeats",Modifier.padding(horizontal=13.dp),fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White)
                                OutlinedButton(onClick={waitlistSeats=(waitlistSeats+1).coerceAtMost(waitlistLimit)},enabled=waitlistSeats<waitlistLimit,contentPadding=PaddingValues(horizontal=13.dp,vertical=3.dp)){Text("+",fontSize=18.sp,color=Color.White)}
                            }
                            OutlinedButton(onClick={scope.launch{
                                busy=true;msg=""
                                try {
                                    val entry=AvClient.api.joinWaitingList(WaitingListRequest(showId,waitlistSeats))
                                    waitingEntries=listOf(entry)+waitingEntries.filterNot{it.id==entry.id}
                                    msg=if(entry.status.equals("OFFERED",true)) "Seats are available for you to claim now." else "You're on the waiting list${entry.position?.let{" at position $it"}.orEmpty()}."
                                } catch(t:Throwable) { msg=apiErrorMessage(t) }
                                finally { busy=false }
                            }},Modifier.fillMaxWidth().padding(bottom=12.dp),enabled=!busy,border=BorderStroke(1.dp,Red),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)) { Text(if(busy)"Joining waiting list…" else "Join Waiting List",fontWeight=FontWeight.Bold) }
                        }
                    } else {
                        val offered=currentEntry.status.equals("OFFERED",true)
                        val expired=offered&&waitingOfferExpired(currentEntry)
                        Text(buildString {
                            append(if(offered) "Seats offered" else "Waiting")
                            currentEntry.position?.let{append(" · position $it")}
                            append(" · ${currentEntry.requestedSeatsCount} ticket${if(currentEntry.requestedSeatsCount==1)"" else "s"}")
                        },Modifier.fillMaxWidth().padding(top=8.dp,bottom=4.dp),fontSize=12.sp,color=if(offered&&!expired)Color(0xFF50D897) else Muted,textAlign=TextAlign.Center)
                        if(offered&&currentEntry.offerExpiresAt!=null) Text("Offer expires ${displayTimestamp(currentEntry.offerExpiresAt)}",Modifier.fillMaxWidth().padding(bottom=6.dp),fontSize=11.sp,color=if(expired)Red else Muted,textAlign=TextAlign.Center)
                        if(offered&&!expired) Button(onClick={scope.launch{
                            busy=true;msg=""
                            try {
                                val claimed=AvClient.api.claimWaitingList(currentEntry.id)
                                waitingEntries=AvClient.api.waitingList().filter{it.showId==showId}
                                if(claimed.status.equals("CONVERTED",true)) nav.navigate("bookings") { launchSingleTop=true }
                                else msg="Your offer is ${claimed.status.lowercase()}. Refresh shortly for the latest status."
                            } catch(t:Throwable) { msg=apiErrorMessage(t) }
                            finally { busy=false }
                        }},Modifier.fillMaxWidth().padding(bottom=6.dp),enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Red)) { Text(if(busy)"Claiming offer…" else "Claim ${currentEntry.requestedSeatsCount} seat${if(currentEntry.requestedSeatsCount==1)"" else "s"}",color=Color.White) }
                        OutlinedButton(onClick={scope.launch{
                            busy=true
                            try { AvClient.api.cancelWaitingList(currentEntry.id);waitingEntries=waitingEntries.filterNot{it.id==currentEntry.id};msg="Waiting-list request cancelled." }
                            catch(t:Throwable) { msg=apiErrorMessage(t) }
                            finally { busy=false }
                        }},Modifier.fillMaxWidth().padding(bottom=12.dp),enabled=!busy,border=BorderStroke(1.dp,Color(0xFF596168)),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)) { Text("Leave Waiting List") }
                    }
                }
            }
        }
    }
}

@Composable private fun SeatLegend(color:Color,label:String) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(15.dp).border(1.dp,color,RoundedCornerShape(4.dp)).background(if(label=="Selected")color else color.copy(alpha=.18f),RoundedCornerShape(4.dp)))
        Text(label,Modifier.padding(start=5.dp),fontSize=10.sp,color=Color(0xFFD0D5D9))
    }
}
@Composable private fun BookingTermsScreen(nav:androidx.navigation.NavHostController,showId:String,seatCsv:String,holdExpiry:String) {
    val seatIds=remember(showId,seatCsv){seatCsv.split(',').map{it.trim()}.filter{it.isNotBlank()}.distinct()}
    var currentIndex by remember(showId,seatCsv){mutableIntStateOf(0)}
    var accepted by remember(showId,seatCsv){mutableStateOf(setOf<Int>())}
    var availability by remember(showId){mutableStateOf<AvailabilityDto?>(null)}
    var show by remember(showId){mutableStateOf<Show?>(null)}
    var movie by remember(showId){mutableStateOf<Movie?>(null)}
    var theatre by remember(showId){mutableStateOf<Theatre?>(null)}
    var loading by remember(showId){mutableStateOf(true)}
    var loadError by remember(showId){mutableStateOf("")}
    var alternativeMobile by remember(showId,seatCsv){mutableStateOf("")}
    var isSubmitting by remember(showId,seatCsv){mutableStateOf(false)}
    var submitError by remember(showId,seatCsv){mutableStateOf("")}
    var createdBooking by remember(showId,seatCsv){mutableStateOf<BookingRecord?>(null)}
    var holdExpired by remember(showId,seatCsv){mutableStateOf(false)}
    var secondsLeft by remember(showId,seatCsv,holdExpiry){mutableLongStateOf(holdExpirySeconds(holdExpiry))}
    var refreshAvailability by remember(showId){mutableIntStateOf(0)}
    var showLeaveDialog by remember(showId,seatCsv){mutableStateOf(false)}
    val scope= rememberCoroutineScope()
    BackHandler(enabled=createdBooking==null) { showLeaveDialog=true }

    LaunchedEffect(showId) {
        loading=true;loadError=""
        try {
            val loadedAvailability=parseAvailability(AvClient.api.availability(showId))
            availability=loadedAvailability
            val loadedShow=loadedAvailability.show?:AvClient.api.show(showId)
            show=loadedShow
            movie=AvClient.api.movie(loadedShow.movieId)
            theatre=AvClient.api.theatre(loadedShow.theatreId)
        } catch(t:Throwable) { loadError=t.message?:"Unable to load booking details" }
        finally { loading=false }
    }

    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner,showId) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_RESUME)refreshAvailability++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(showId,refreshAvailability) {
        if(refreshAvailability>0) try { availability=parseAvailability(AvClient.api.availability(showId)) } catch(_:Throwable) { }
    }
    LaunchedEffect(showId,seatCsv,holdExpiry) {
        if(holdExpiry.isBlank()) return@LaunchedEffect
        while(isActive) {
            secondsLeft=holdExpirySeconds(holdExpiry)
            if(secondsLeft<=0L) {
                holdExpired=true
                try { availability=parseAvailability(AvClient.api.availability(showId)) } catch(_:Throwable) { }
                break
            }
            delay(1000)
        }
    }

    val selectedSeatData=availability?.layout?.seats.orEmpty().filter{it.id in seatIds}
    val selectedLabels=selectedSeatData.map{it.label}.filter{it.isNotBlank()}
    val resolvedShow=show
    val totalAmount=if(resolvedShow?.ticketType.equals("FREE",true)) 0.0 else seatIds.sumOf{id->
        val seat=selectedSeatData.firstOrNull{it.id==id}
        seat?.category?.let{resolvedShow?.seatPrices?.get(it)}?:resolvedShow?.ticketPrice?:0.0
    }
    val bookingReady=!loading&&loadError.isBlank()&&resolvedShow!=null&&movie!=null&&theatre!=null&&seatIds.isNotEmpty()&&selectedSeatData.size==seatIds.size
    val mobileDigits=alternativeMobile.count{it.isDigit()}
    val alternativeMobileValid=alternativeMobile.isBlank()||mobileDigits in 7..15
    val allAccepted=accepted.size==BookingConditions.size

    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.showtimes_cinema_bg),"",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0xEB050708)))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp,vertical=5.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={showLeaveDialog=true},modifier=Modifier.size(44.dp)){Icon(Icons.Default.ArrowBack,"Back",tint=Color.White)}
                Image(painterResource(R.drawable.av_logo),"Audience Verdict",Modifier.width(145.dp).height(42.dp),contentScale=ContentScale.Fit)
            }
            Text("IMPORTANT BEFORE YOU CONFIRM",Modifier.padding(top=7.dp),fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp,color=Red)
            Text("Terms & Conditions",Modifier.padding(top=3.dp),fontSize=27.sp,fontWeight=FontWeight.Black,color=Color.White)
            Text("Please review and agree to each condition for a smooth movie experience.",Modifier.padding(top=4.dp),fontSize=12.sp,lineHeight=17.sp,color=Color(0xFFCDD2D7))
            BookingProgress()
            if(holdExpiry.isNotBlank()) Text(if(holdExpired)"Seat hold expired. Return to seat selection to check availability again." else "Seats reserved for ${secondsLeft/60}:${(secondsLeft%60).toString().padStart(2,'0')}",Modifier.fillMaxWidth().padding(top=3.dp),fontSize=11.sp,color=if(holdExpired)Red else Color(0xFF55D993),textAlign=TextAlign.Center)

            Column(Modifier.fillMaxWidth().padding(top=8.dp).border(1.dp,Color(0xFF2B3238),RoundedCornerShape(16.dp)).background(Color(0xD9080B0D),RoundedCornerShape(16.dp)).padding(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Your Booking Details",Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Text("Edit",Modifier.clickable{nav.popBackStack()}.padding(horizontal=8.dp,vertical=6.dp),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Red)
                }
                if(loading) Busy()
                else if(loadError.isNotBlank()) Text("Booking details could not be loaded: $loadError",Modifier.padding(top=8.dp),fontSize=12.sp,color=Color(0xFFFF777D))
                else {
                    HorizontalDivider(Modifier.padding(vertical=9.dp),color=Color(0xFF293137))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        val poster=movie?.posterUrl
                        if(!poster.isNullOrBlank()) Poster(poster,Modifier.width(66.dp).height(92.dp).clip(RoundedCornerShape(8.dp)))
                        else Box(Modifier.width(66.dp).height(92.dp).background(Panel2,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Movie,"Movie poster",tint=Muted)}
                        Column(Modifier.weight(1f).padding(start=10.dp)) {
                            Text(movie?.title?:"Movie",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=2)
                            Text(listOfNotNull(movie?.language?:movie?.originalLanguage,(movie?.durationMinutes?:movie?.duration)?.let{"${it/60}h ${it%60}m"}).joinToString("  ·  ").ifBlank{"Movie details"},Modifier.padding(top=5.dp),fontSize=11.sp,color=Color(0xFFC5CBD0))
                            Text(theatre?.name?:"Cinema",Modifier.padding(top=8.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1)
                            Text(listOfNotNull(theatre?.address,theatre?.city).joinToString(", "),Modifier.padding(top=2.dp),fontSize=10.sp,color=Muted,maxLines=2,lineHeight=14.sp)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical=9.dp),color=Color(0xFF293137))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        BookingDetail("DATE & TIME",listOfNotNull(show?.date?.let(::displayMovieDate),show?.startTime).joinToString(" · ").ifBlank{"—"},Modifier.weight(1f))
                        BookingDetail("SEATS",selectedLabels.joinToString(", ").ifBlank{"${seatIds.size} selected"},Modifier.weight(1f))
                        BookingDetail("TOTAL",java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en","IN")).format(totalAmount),Modifier.weight(1f))
                    }
                    OutlinedTextField(alternativeMobile,{alternativeMobile=it.filter{ch->ch.isDigit()||ch=='+'}.take(16)},Modifier.fillMaxWidth().padding(top=7.dp),singleLine=true,label={Text("Alternative mobile (optional)")},placeholder={Text("Add a contact number")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),colors=fieldColors())
                    if(!alternativeMobileValid) Text("Enter a valid phone number or leave this field blank.",Modifier.padding(top=3.dp),fontSize=10.sp,color=Color(0xFFFF777D))
                }
            }

            if(createdBooking==null) {
                val condition=BookingConditions[currentIndex]
                val conditionText=if(currentIndex==2) "No cancellation or refund policy for this show was supplied by the cinema. Please contact the venue to confirm the applicable terms before booking." else condition.description
                Column(Modifier.fillMaxWidth().padding(top=12.dp).border(1.5.dp,Red,RoundedCornerShape(20.dp)).background(Color(0xE9090B0D),RoundedCornerShape(20.dp)).pointerInput(currentIndex){
                    var drag=0f
                    detectHorizontalDragGestures(onHorizontalDrag={change,amount->drag+=amount;change.consume()},onDragEnd={if(drag < -55f)currentIndex=(currentIndex+1).coerceAtMost(BookingConditions.lastIndex) else if(drag>55f)currentIndex=(currentIndex-1).coerceAtLeast(0);drag=0f})
                }.padding(11.dp)) {
                    Box(Modifier.fillMaxWidth().height(178.dp).clip(RoundedCornerShape(13.dp))) {
                        Image(painterResource(condition.imageRes),condition.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x11000000),Color(0xC9000000)))))
                        Text("%02d / %02d".format(currentIndex+1,BookingConditions.size),Modifier.align(Alignment.TopStart).padding(10.dp).background(Red,RoundedCornerShape(50.dp)).padding(horizontal=12.dp,vertical=8.dp),fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    }
                    Text(condition.title,Modifier.padding(start=3.dp,end=3.dp,top=12.dp),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Text(conditionText,Modifier.padding(horizontal=3.dp,vertical=7.dp),fontSize=12.sp,lineHeight=17.sp,color=Color(0xFFD1D6DB))
                    Row(Modifier.fillMaxWidth().clickable{accepted=if(currentIndex in accepted)accepted-currentIndex else accepted+currentIndex}.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=currentIndex in accepted,onCheckedChange={checked->accepted=if(checked)accepted+currentIndex else accepted-currentIndex},colors=CheckboxDefaults.colors(checkedColor=Red,uncheckedColor=Color(0xFFB6BEC5),checkmarkColor=Color.White))
                        Text("I agree to this condition.",Modifier.padding(start=4.dp),fontSize=13.sp,color=Color.White)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={currentIndex=(currentIndex-1).coerceAtLeast(0)},enabled=currentIndex>0,modifier=Modifier.size(42.dp).border(1.dp,if(currentIndex>0)Red else Color(0xFF34393E),CircleShape)){Icon(Icons.Default.ArrowBack,"Previous condition",tint=if(currentIndex>0)Red else Muted)}
                    Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically) {
                        BookingConditions.indices.forEach{idx->Box(Modifier.size(if(idx==currentIndex)11.dp else 8.dp).background(if(idx==currentIndex)Red else Color(0xFF41474D),CircleShape).clickable{currentIndex=idx})}
                    }
                    IconButton(onClick={currentIndex=(currentIndex+1).coerceAtMost(BookingConditions.lastIndex)},enabled=currentIndex<BookingConditions.lastIndex,modifier=Modifier.size(42.dp).border(1.dp,if(currentIndex<BookingConditions.lastIndex)Red else Color(0xFF34393E),CircleShape)){Icon(Icons.Default.ArrowForward,"Next condition",tint=if(currentIndex<BookingConditions.lastIndex)Red else Muted)}
                }
                Text("${accepted.size} of ${BookingConditions.size} conditions accepted",Modifier.fillMaxWidth().padding(top=1.dp,bottom=3.dp),fontSize=11.sp,color=if(allAccepted)Color(0xFF31D27E) else Muted,textAlign=TextAlign.Center)
                if(submitError.isNotBlank()) Text(submitError,Modifier.fillMaxWidth().padding(vertical=3.dp),fontSize=12.sp,color=Color(0xFFFF777D),textAlign=TextAlign.Center)
                val canConfirm=allAccepted&&bookingReady&&alternativeMobileValid&&!isSubmitting&&!holdExpired
                val confirmBrush=if(canConfirm)Brush.horizontalGradient(listOf(Color(0xFFFF555B),Color(0xFFFF202A),Color(0xFFE91F29))) else Brush.horizontalGradient(listOf(Color(0xFF542528),Color(0xFF3D2426)))
                Box(Modifier.fillMaxWidth().padding(top=6.dp,bottom=12.dp).height(54.dp).clip(RoundedCornerShape(16.dp)).background(confirmBrush)) {
                    Button(onClick={
                        if(!allAccepted||!bookingReady||isSubmitting)return@Button
                        scope.launch {
                            isSubmitting=true;submitError=""
                            try {
                                val response=AvClient.api.createBooking(BookingPayload(showId,seatIds,termsAccepted=true,alternativeMobile=alternativeMobile.trim().ifBlank{null}))
                                if(!response.has("id")||response.get("id").isJsonNull||response.get("id").asString.isBlank()) throw IllegalStateException("The server did not return a booking confirmation.")
                                val booking=com.google.gson.Gson().fromJson(response,BookingRecord::class.java)
                                createdBooking=booking
                                nav.navigate("booking-ticket/${Uri.encode(booking.id)}") { launchSingleTop=true }
                            } catch(t:Throwable) { submitError=t.message?:"Booking could not be confirmed. Please try again." }
                            finally { isSubmitting=false }
                        }
                    },Modifier.fillMaxSize(),enabled=canConfirm,colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent,disabledContainerColor=Color.Transparent),shape=RoundedCornerShape(16.dp)) {
                        Text(if(isSubmitting)"Submitting booking..." else "Confirm booking  ->",fontSize=16.sp,fontWeight=FontWeight.Bold,color=if(canConfirm)Color.White else Color(0xFF999093))
                    }
                }
            } else {
                val booking=createdBooking!!
                val confirmed=booking.status.equals("CONFIRMED",true)
                Column(Modifier.fillMaxWidth().padding(top=20.dp).border(1.dp,Color(0xFF245E45),RoundedCornerShape(18.dp)).background(Color(0xD9090F0D),RoundedCornerShape(18.dp)).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(if(confirmed)Icons.Default.CheckCircle else Icons.Default.Schedule,if(confirmed)"Booking confirmed" else "Booking received",tint=if(confirmed)Color(0xFF31D27E) else Color(0xFFFFB547),modifier=Modifier.size(54.dp))
                    Text(if(confirmed)"Booking Confirmed" else "Booking Request Received",Modifier.padding(top=12.dp),fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center)
                    Text(if(confirmed)"Your booking was confirmed by the cinema." else "The booking was created by the server with status ${booking.status.ifBlank{"PENDING"}}.",Modifier.padding(top=6.dp),fontSize=13.sp,color=Color(0xFFD0D5D9),textAlign=TextAlign.Center)
                    if(!booking.ticketCode.isNullOrBlank()) Text("Ticket code: ${booking.ticketCode}",Modifier.padding(top=12.dp),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
                    Button(onClick={nav.navigate("bookings")},Modifier.fillMaxWidth().padding(top=18.dp),colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("View My Bookings",color=Color.White)}
                }
            }
        }
    }
    if(showLeaveDialog) AlertDialog(
        onDismissRequest={showLeaveDialog=false},
        title={Text("Leave booking?",color=Color.White)},
        text={Text("Your booking will not be submitted. Your temporary seat hold will expire and the seats will be released when the timer ends. You can return to seat selection to check availability.",color=Color(0xFFD0D5D9))},
        confirmButton={TextButton(onClick={showLeaveDialog=false;nav.popBackStack()}){Text("Leave booking",color=Red)}},
        dismissButton={TextButton(onClick={showLeaveDialog=false}){Text("Keep booking",color=Color.White)}},
        containerColor=Panel
    )
}

@Composable private fun BookingProgress() {
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        listOf("Theatre & Show","Seats","Confirm").forEachIndexed{index,label->
            if(index>0) Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF656B70)))
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(if(index==2)Red else Color(0xFF321519),CircleShape).border(1.dp,Red,CircleShape),contentAlignment=Alignment.Center){Text(if(index<2)"✓" else "3",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)}
                Text(label,Modifier.padding(start=5.dp),fontSize=10.sp,fontWeight=if(index==2)FontWeight.Bold else FontWeight.Normal,color=if(index==2)Red else Color(0xFFD0D5D9),maxLines=1)
            }
        }
    }
}

@Composable private fun BookingDetail(label:String,value:String,modifier:Modifier=Modifier) {
    Column(modifier.background(Color(0xFF111619),RoundedCornerShape(9.dp)).padding(horizontal=8.dp,vertical=8.dp)) {
        Text(label,fontSize=8.sp,letterSpacing=.7.sp,color=Muted,maxLines=1)
        Text(value,Modifier.padding(top=4.dp),fontSize=10.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=2,lineHeight=13.sp)
    }
}

@Composable private fun Bookings(nav:androidx.navigation.NavHostController){
    var error by remember{mutableStateOf("")}
    var rows by remember{mutableStateOf<List<BookingRecord>>(emptyList())}
    var refresh by remember{mutableIntStateOf(0)}
    var selectedToCancel by remember{mutableStateOf<BookingRecord?>(null)}
    var cancellationSeats by remember{mutableStateOf<List<String>>(emptyList())}
    var cancellationLabels by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    var cancellationSelection by remember{mutableStateOf<Set<String>>(emptySet())}
    var cancelBusy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    val context=LocalContext.current
    val scope= rememberCoroutineScope()
    LaunchedEffect(refresh){try{rows=AvClient.api.bookings();error=""}catch(t:Throwable){error=apiErrorMessage(t,context)}}
    LaunchedEffect(selectedToCancel?.id) {
        val booking=selectedToCancel ?: return@LaunchedEffect
        cancellationSeats=booking.seatIds
        cancellationSelection=emptySet()
        cancellationLabels=runCatching {
            val availability=parseAvailability(AvClient.api.availability(booking.showId))
            availability.layout?.seats.orEmpty().associate { it.id to it.label }
        }.getOrDefault(emptyMap())
    }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("My Bookings",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Color.White)
        if(message.isNotBlank()) Text(message,Modifier.fillMaxWidth().padding(top=10.dp).background(Color(0xFF123020),RoundedCornerShape(10.dp)).padding(11.dp),fontSize=12.sp,color=if(message.startsWith("Booking cancelled"))Color(0xFF7CE0A4) else Red)
        if(error.isNotBlank()) ErrorCard(error)
        else if(rows.isEmpty()) Text("No bookings found.",color=Muted,modifier=Modifier.padding(top=20.dp))
        else rows.forEach{booking->BookingCard(booking,{nav.navigate("booking-ticket/${Uri.encode(booking.id)}")},{selectedToCancel=booking})}
    }
    val selected=selectedToCancel
    if(selected!=null) AlertDialog(
        onDismissRequest={if(!cancelBusy)selectedToCancel=null},
        title={Text("Choose seats to cancel",color=Color.White,fontWeight=FontWeight.Bold)},
        text={
            Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
                Text("Select each seat you want to cancel. Only selected seats will be sent for cancellation. Refunds follow the cinema's policy.",color=Color(0xFFD0D5D9),fontSize=13.sp)
                Spacer(Modifier.height(10.dp))
                cancellationSeats.forEachIndexed { index, seatId ->
                    val label=cancellationLabels[seatId]?.takeIf{it.isNotBlank()} ?: "Seat ${index+1}"
                    Row(Modifier.fillMaxWidth().clickable(enabled=!cancelBusy) {
                        cancellationSelection=if(seatId in cancellationSelection)cancellationSelection-seatId else cancellationSelection+seatId
                    }.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=seatId in cancellationSelection,onCheckedChange={checked->cancellationSelection=if(checked)cancellationSelection+seatId else cancellationSelection-seatId},enabled=!cancelBusy,colors=CheckboxDefaults.colors(checkedColor=Red,checkmarkColor=Color.White,uncheckedColor=Muted))
                        Text(label,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                    }
                }
                Text("${cancellationSelection.size} of ${cancellationSeats.size} seats selected",Modifier.padding(top=6.dp),fontSize=12.sp,color=Muted)
            }
        },
        confirmButton={TextButton(enabled=!cancelBusy&&cancellationSelection.isNotEmpty(),onClick={scope.launch{
            cancelBusy=true;message=""
            try {
                val ids=cancellationSeats.filter{it in cancellationSelection}
                val updated=AvClient.api.cancelBooking(selected.id,CancelBookingPayload(ids))
                rows=rows.map{if(it.id==selected.id)updated else it}
                message=if(updated.seatIds.isEmpty()||updated.status.equals("CANCELLED",true))"Booking cancelled." else "${ids.size} seat${if(ids.size==1)"" else "s"} cancelled."
                selectedToCancel=null
            } catch(t:Throwable) { message=apiErrorMessage(t,context) }
            finally { cancelBusy=false }
        }}){if(cancelBusy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Red) else Text("Cancel selected",color=Red)}},
        dismissButton={TextButton(enabled=!cancelBusy,onClick={selectedToCancel=null}){Text("Keep booking",color=Color.White)}},
        containerColor=Panel
    )
}

@Composable private fun BookingCard(booking:BookingRecord,onOpen:()->Unit,onCancel:()->Unit) {
    var seatLabels by remember(booking.id){mutableStateOf<List<String>?>(null)}
    LaunchedEffect(booking.showId,booking.seatIds) {
        seatLabels=try { loadSeatLabels(booking.showId,booking.seatIds) } catch(_:Throwable) { emptyList() }
    }
    val status=booking.status.replace('_',' ').lowercase().replaceFirstChar{it.uppercase()}.ifBlank{"Booking"}
    val statusColor=when(booking.status.uppercase()){"CONFIRMED"->Color(0xFF20C77A);"CANCELLED"->Red;else->Color(0xFFFFB547)}
    val whenText=listOfNotNull(booking.date?.takeIf{it.isNotBlank()}?.let(::displayMovieDate),booking.time?.takeIf{it.isNotBlank()}).joinToString(" ? ")
    val amount=java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en","IN")).format(booking.totalAmount)
    Column(Modifier.fillMaxWidth().padding(top=12.dp).background(Panel,RoundedCornerShape(14.dp)).clickable(onClick=onOpen).padding(15.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(booking.movie?.takeIf{it.isNotBlank()}?:"Movie booking",Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.Bold,color=Color.White)
            Text(status,Modifier.background(statusColor.copy(alpha=.16f),RoundedCornerShape(20.dp)).padding(horizontal=10.dp,vertical=5.dp),fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=statusColor)
        }
        Text(booking.theatre?.takeIf{it.isNotBlank()}?:"Theatre details unavailable",Modifier.padding(top=8.dp),fontSize=13.sp,color=Color(0xFFD1D5D8))
        val venueDetails=listOfNotNull(booking.screen?.takeIf{it.isNotBlank()},whenText.takeIf{it.isNotBlank()}).joinToString(" ? ")
        if(venueDetails.isNotBlank()) Text(venueDetails,Modifier.padding(top=3.dp),fontSize=12.sp,color=Muted)
        HorizontalDivider(Modifier.padding(vertical=12.dp),color=Color(0xFF30383D))
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${booking.ticketCount} ${if(booking.ticketCount==1)"ticket" else "tickets"}",fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
                Text("Seats: ${seatLabels?.takeIf{it.isNotEmpty()}?.joinToString(", ") ?: if(seatLabels==null)"Loading seat numbers?" else "Seat labels unavailable"}",Modifier.padding(top=2.dp),fontSize=11.sp,color=Muted,maxLines=2)
            }
            Text(amount,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)
        }
        if(!booking.ticketCode.isNullOrBlank()) Text("Ticket code  ${booking.ticketCode}",Modifier.fillMaxWidth().padding(top=12.dp).background(Panel2,RoundedCornerShape(8.dp)).padding(10.dp),fontSize=12.sp,color=Color(0xFFD1D5D8))
        Text("View ticket  ?",Modifier.align(Alignment.End).padding(top=8.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Red)
        if(booking.status.equals("CANCELLED",true)&&!booking.cancellationReason.isNullOrBlank()) Text(booking.cancellationReason,Modifier.padding(top=8.dp),fontSize=12.sp,color=Red)
        if(booking.status.uppercase() !in setOf("CANCELLED","EXPIRED","REJECTED")&&!booking.attended) OutlinedButton(onClick=onCancel,Modifier.fillMaxWidth().padding(top=8.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Red),colors=ButtonDefaults.outlinedButtonColors(contentColor=Red),shape=RoundedCornerShape(10.dp)){Text("Cancel ticket",fontWeight=FontWeight.SemiBold)}
    }
}

@Composable private fun BookingTicketScreen(nav:androidx.navigation.NavHostController,bookingId:String) {
    var booking by remember(bookingId){mutableStateOf<BookingRecord?>(null)}
    var seatLabels by remember(bookingId){mutableStateOf<List<String>>(emptyList())}
    var qrBitmap by remember(bookingId){mutableStateOf<Bitmap?>(null)}
    var movieInfo by remember(bookingId){mutableStateOf<Movie?>(null)}
    var showInfo by remember(bookingId){mutableStateOf<Show?>(null)}
    var theatreInfo by remember(bookingId){mutableStateOf<Theatre?>(null)}
    var error by remember(bookingId){mutableStateOf("")}
    var loading by remember(bookingId){mutableStateOf(true)}
    var pdfBusy by remember(bookingId){mutableStateOf(false)}
    var pdfError by remember(bookingId){mutableStateOf("")}
    var pendingPdf by remember{mutableStateOf<ByteArray?>(null)}
    val context=LocalContext.current
    val scope= rememberCoroutineScope()
    val pdfLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
        val bytes=pendingPdf
        if(uri!=null&&bytes!=null)runCatching{context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:throw IllegalStateException("Could not open the selected file")}.onFailure{pdfError="Ticket PDF could not be saved: ${it.message.orEmpty()}"}
        pendingPdf=null;pdfBusy=false
    }
    LaunchedEffect(bookingId) {
        try {
            val loaded=AvClient.api.booking(bookingId);booking=loaded
            seatLabels=runCatching{loadSeatLabels(loaded.showId,loaded.seatIds)}.getOrDefault(emptyList())
            val loadedShow=runCatching{AvClient.api.show(loaded.showId)}.getOrNull();showInfo=loadedShow
            loadedShow?.let{show->movieInfo=runCatching{AvClient.api.movie(show.movieId)}.getOrNull();theatreInfo=runCatching{AvClient.api.theatre(show.theatreId)}.getOrNull()}
            if(loaded.status.equals("CONFIRMED",true))qrBitmap=runCatching{val bytes=AvClient.api.bookingQr(bookingId).bytes();android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)}.getOrNull()
        }
        catch(t:Throwable) { error=apiErrorMessage(t) }
        finally { loading=false }
    }
    val current=booking
    val confirmed=current?.status.equals("CONFIRMED",true)
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)) {
        PageHeader(if(confirmed)"Booking Confirmed" else "Booking Ticket"){nav.popBackStack()}
        Image(painterResource(R.drawable.av_logo),"Audience Verdict",Modifier.align(Alignment.CenterHorizontally).width(190.dp).height(74.dp),contentScale=ContentScale.Fit)
        if(loading) Busy()
        else if(error.isNotBlank()) ErrorCard(error)
        else if(current==null) ErrorCard("Booking details are unavailable.")
        else {
            Column(Modifier.fillMaxWidth().padding(top=10.dp).border(1.dp,if(confirmed)Color(0xFF1DBD70) else Color(0xFFFFB547),RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xFF15191B),Color(0xFF090B0C))),RoundedCornerShape(18.dp)).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(if(confirmed)Icons.Default.CheckCircle else Icons.Default.Schedule,if(confirmed)"Booking confirmed" else "Booking status",tint=if(confirmed)Color(0xFF20D57C) else Color(0xFFFFB547),modifier=Modifier.size(60.dp))
                Text(if(confirmed)"Booking Confirmed!" else "Booking ${current.status.lowercase().replaceFirstChar{it.uppercase()}}",Modifier.padding(top=8.dp),fontSize=25.sp,fontWeight=FontWeight.Black,color=Color.White,textAlign=TextAlign.Center)
                Text(if(confirmed)"Your movie tickets have been booked successfully." else "Ticket validity will update when the cinema confirms this booking.",Modifier.padding(top=5.dp),fontSize=13.sp,color=Color(0xFFD0D5D9),textAlign=TextAlign.Center)
                if(!current.ticketCode.isNullOrBlank()) Text("TICKET CODE  ${current.ticketCode}",Modifier.padding(top=14.dp).border(1.dp,Red,RoundedCornerShape(24.dp)).padding(horizontal=16.dp,vertical=9.dp),fontSize=13.sp,fontWeight=FontWeight.Bold,color=Color.White)
            }
            Column(Modifier.fillMaxWidth().padding(top=12.dp).border(1.dp,Color(0xFF30383D),RoundedCornerShape(16.dp)).background(Panel,RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text(current.movie?:"Movie",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color.White)
                Text(current.theatre?:"Cinema",Modifier.padding(top=6.dp),fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFFD5D9DD))
                Text(listOfNotNull(current.screen,current.date?.let(::displayMovieDate),current.time).joinToString("  ·  "),Modifier.padding(top=3.dp),fontSize=12.sp,color=Muted)
                HorizontalDivider(Modifier.padding(vertical=12.dp),color=Color(0xFF30383D))
                TicketInfoRow("Seats", "${current.seatIds.size} ticket${if(current.seatIds.size==1)"" else "s"}  ·  ${seatLabels.joinToString(", ").ifBlank{"Seat labels unavailable"}}")
                TicketInfoRow("Total",java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en","IN")).format(current.totalAmount))
                TicketInfoRow("Booking reference",current.id)
            }
            if(confirmed) {
                Column(Modifier.fillMaxWidth().padding(top=12.dp).border(1.dp,Red.copy(alpha=.75f),RoundedCornerShape(16.dp)).background(Color(0xFF1A0A0C),RoundedCornerShape(16.dp)).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("Show this QR code at the theatre",fontSize=17.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center)
                    Text("Scan the ticket QR issued by Audience Verdict",Modifier.padding(top=4.dp),fontSize=11.sp,color=Muted,textAlign=TextAlign.Center)
                    qrBitmap?.let{Image(it.asImageBitmap(),"Backend issued booking ticket QR",Modifier.padding(top=12.dp).size(210.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(8.dp))}?:Text("The ticket QR is not available from the server yet. Refresh this ticket shortly.",Modifier.padding(16.dp),color=Red,textAlign=TextAlign.Center)
                }
            } else if(current.ticketCode.isNullOrBlank()) Text("A ticket code will appear after the booking is confirmed.",Modifier.padding(16.dp),fontSize=12.sp,color=Muted)
            if(confirmed&&qrBitmap!=null) Button(onClick={
                scope.launch {
                    pdfBusy=true;pdfError=""
                    try {
                        val posterUrl=movieInfo?.posterUrl?:movieInfo?.posterImages?.firstOrNull()
                        val poster=if(posterUrl.isNullOrBlank())null else withContext(Dispatchers.IO){runCatching{
                            val request=coil.request.ImageRequest.Builder(context).data(posterUrl).allowHardware(false).build()
                            val result=coil.ImageLoader(context).execute(request)
                            val drawable=result.drawable
                            if(drawable is android.graphics.drawable.BitmapDrawable)drawable.bitmap else drawable?.let{d->Bitmap.createBitmap(d.intrinsicWidth.coerceAtLeast(1),d.intrinsicHeight.coerceAtLeast(1),Bitmap.Config.ARGB_8888).also{b->val c=android.graphics.Canvas(b);d.setBounds(0,0,c.width,c.height);d.draw(c)}}
                        }.getOrNull()}
                        pendingPdf=withContext(Dispatchers.IO){makeTicketPdf(context,current,seatLabels,movieInfo,showInfo,theatreInfo,qrBitmap,poster)}
                        pdfLauncher.launch("AudienceVerdict-${current.ticketCode?:current.id}.pdf")
                    } catch(t:Throwable) {pdfError="Ticket PDF could not be prepared: ${t.message.orEmpty()}";pdfBusy=false}
                }
            },Modifier.fillMaxWidth().padding(top=10.dp),enabled=!pdfBusy,colors=ButtonDefaults.buttonColors(containerColor=Red),shape=RoundedCornerShape(14.dp)){if(pdfBusy)CircularProgressIndicator(Modifier.size(18.dp),color=Color.White)else Text("Download Ticket PDF",fontWeight=FontWeight.Bold,color=Color.White)}
            if(pdfError.isNotBlank())Text(pdfError,Modifier.fillMaxWidth().padding(top=6.dp),fontSize=12.sp,color=Red)
            Button(onClick={nav.navigate("bookings"){launchSingleTop=true}},Modifier.fillMaxWidth().padding(top=14.dp),colors=ButtonDefaults.buttonColors(containerColor=Red),shape=RoundedCornerShape(14.dp)){Text("View My Bookings",fontWeight=FontWeight.Bold,color=Color.White)}
            Button(onClick={nav.navigate("home"){popUpTo("home"){inclusive=false};launchSingleTop=true}},Modifier.fillMaxWidth().padding(top=8.dp,bottom=18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF202529)),shape=RoundedCornerShape(14.dp)){Text("Back to Home",color=Color.White)}
        }
    }
}

@Composable private fun TicketInfoRow(label:String,value:String) {
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.Top) {
        Text(label,Modifier.width(100.dp),fontSize=11.sp,color=Muted)
        Text(value,Modifier.weight(1f),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
    }
}

@Composable private fun ReviewPage(movieId:String,nav:androidx.navigation.NavHostController){var reviews by remember{mutableStateOf<List<Review>>(emptyList())};var error by remember{mutableStateOf("")};LaunchedEffect(movieId){try{reviews=AvClient.api.reviews(movieId)}catch(t:Throwable){error=t.message?:"Unable to load reviews"}};Column(Modifier.fillMaxSize().background(Ink)){PageHeader("Audience Reviews"){nav.popBackStack()};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp)){if(error.isNotBlank())ErrorCard(error);if(reviews.isEmpty()&&error.isBlank())Text("No published reviews yet.",color=Color(0xFFD0D5D9),modifier=Modifier.padding(18.dp));reviews.forEach{r->Column(Modifier.fillMaxWidth().padding(vertical=6.dp).background(Panel,RoundedCornerShape(12.dp)).padding(14.dp)){Text(r.title?:"Review",fontWeight=FontWeight.Bold,color=Color.White);Text("${r.author?:"Audience member"} · ${r.rating?:"—"}",color=Color(0xFFFFB51B),fontSize=12.sp);Text(r.text.orEmpty(),color=Color(0xFFD0D5D9),modifier=Modifier.padding(top=6.dp));Text(r.status?:"",color=Muted,fontSize=10.sp)}}}}}

@Composable private fun Profile(nav:androidx.navigation.NavHostController,onLogout:()->Unit){
    var me by remember{mutableStateOf<Me?>(null)}
    var notificationCount by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){try{me=AvClient.api.me()}catch(_:Throwable){};try{notificationCount=parseApiList<AvNotification>(AvClient.api.notifications()).count{it.read!=true}}catch(_:Throwable){}}
    Box(Modifier.fillMaxSize().background(Ink)) {
        Image(painterResource(R.drawable.about_audience_hero),"Cinema background",Modifier.fillMaxWidth().height(285.dp),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(290.dp).background(Brush.verticalGradient(listOf(Color(0x66000000),Ink))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top=23.dp,bottom=15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Row(verticalAlignment=Alignment.CenterVertically){Text("My ",fontSize=34.sp,fontWeight=FontWeight.Light,color=Color.White);Text("Profile",fontSize=34.sp,fontWeight=FontWeight.Black,color=Red)};Text("Manage your account and preferences",Modifier.padding(top=2.dp),fontSize=14.sp,color=Color(0xFFD0D5D9))};Box(Modifier.size(54.dp).border(1.dp,Color(0xFF77585B),CircleShape).clickable{nav.navigate("notifications"){launchSingleTop=true}},contentAlignment=Alignment.Center){Icon(Icons.Default.Notifications,"Notifications",tint=Color.White,modifier=Modifier.size(27.dp));if(notificationCount>0)Box(Modifier.align(Alignment.TopEnd).size(11.dp).background(Red,CircleShape))}}
            Row(Modifier.fillMaxWidth().padding(bottom=13.dp).border(1.dp,Red,RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF291315),Color(0xFF101315))),RoundedCornerShape(18.dp)).clickable{nav.navigate("edit-profile")}.padding(17.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(83.dp).border(3.dp,Red,CircleShape).background(Color(0xFF180D0F),CircleShape),contentAlignment=Alignment.Center){Text(me?.name?.firstOrNull()?.uppercase()?:"AV",fontSize=33.sp,color=Color.White)};Column(Modifier.weight(1f).padding(start=15.dp)){Text(me?.name?:"Audience Verdict member",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);Text(me?.email.orEmpty(),Modifier.padding(top=3.dp),fontSize=13.sp,color=Color(0xFFBEC5CA),maxLines=1);Text("Mobile  ${me?.mobile?.takeIf{it.isNotBlank()}?:"Not added"} | ${if(me?.mobileVerified==false)"Not verified" else "Verified"}",Modifier.padding(top=6.dp),fontSize=11.sp,color=if(me?.mobileVerified==false)Muted else Color(0xFF42D392),maxLines=1);if(!me?.alternativeMobile.isNullOrBlank()) Text("Alternative  ${me?.alternativeMobile} | ${if(me?.alternativeMobileVerified==true)"Verified" else "Not verified"}",Modifier.padding(top=3.dp),fontSize=11.sp,color=if(me?.alternativeMobileVerified==true)Color(0xFF42D392) else Muted,maxLines=1);Text("♛  Movie Lover",Modifier.padding(top=9.dp).background(Color(0xFF49161B),RoundedCornerShape(20.dp)).padding(horizontal=10.dp,vertical=5.dp),fontSize=11.sp,color=Color.White)};Icon(Icons.Default.ChevronRight,"Edit profile",tint=Muted)}
            val rows=listOf(Triple(Icons.Default.ConfirmationNumber,"My Bookings","View and manage your bookings") to "bookings",Triple(Icons.Default.Star,"My Reviews","Reviews you have written") to "my-reviews",Triple(Icons.Default.EventAvailable,"Waiting List","See your seat offers and queue") to "waiting-list",Triple(Icons.Default.Notifications,"Notifications","Booking updates and alerts") to "notifications",Triple(Icons.Default.Person,"Edit Profile","Update your personal details") to "edit-profile",Triple(Icons.Default.HeadsetMic,"Help & Support","Get help or contact us") to "help")
            rows.forEach { (item,route)->ProfileMenuCard(item.first,item.second,item.third){nav.navigate(route){launchSingleTop=true}} }
            Row(Modifier.fillMaxWidth().padding(top=15.dp,bottom=12.dp).border(1.dp,Red,RoundedCornerShape(16.dp)).background(Color(0xFF210D10),RoundedCornerShape(16.dp)).clickable(onClick=onLogout).padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Logout,"Logout",tint=Red,modifier=Modifier.size(27.dp));Column(Modifier.padding(start=14.dp)){Text("Logout",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Red);Text("Sign out from your account",fontSize=12.sp,color=Muted)};Spacer(Modifier.weight(1f));Icon(Icons.Default.ChevronRight,"",tint=Muted)}
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable private fun ProfileMenuCard(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp).heightIn(min=83.dp).border(1.dp,Color(0xFF34383D),RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF121719),Color(0xFF1A1012))),RoundedCornerShape(16.dp)).clickable(onClick=onClick).padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(56.dp).background(Color(0xFF411519),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(icon,title,tint=Red,modifier=Modifier.size(29.dp))};Column(Modifier.weight(1f).padding(start=14.dp)){Text(title,fontSize=16.sp,fontWeight=FontWeight.Medium,color=Color.White);Text(subtitle,Modifier.padding(top=4.dp),fontSize=12.sp,color=Color(0xFFBBC2C8))};Icon(Icons.Default.ChevronRight,"Open $title",tint=Muted)}
}

@Composable private fun NotificationsScreen(){
    var items by remember{mutableStateOf<List<AvNotification>>(emptyList())};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(true)}
    var refresh by remember{mutableIntStateOf(0)};val context=LocalContext.current
    val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)refresh++}
    LaunchedEffect(refresh){loading=true;error="";try{items=parseApiList(AvClient.api.notifications());postNewLocalNotifications(context,items)}catch(t:Throwable){error="Notifications could not be loaded. ${t.message.orEmpty()}"}finally{loading=false}}
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Notifications",Modifier.weight(1f),fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);if(android.os.Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)TextButton(onClick={permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)}){Text("Enable phone alerts",color=Red)};TextButton(onClick={refresh++}){Text("Refresh",color=Red)}};when{loading->Busy();error.isNotBlank()->ErrorCard(error);items.isEmpty()->Text("You're all caught up. Booking updates and account alerts will appear here.",Modifier.padding(18.dp),fontSize=14.sp,color=Muted);else->items.forEach{item->Row(Modifier.fillMaxWidth().padding(vertical=5.dp).border(1.dp,Color(0xFF33393E),RoundedCornerShape(14.dp)).background(Panel,RoundedCornerShape(14.dp)).padding(14.dp),verticalAlignment=Alignment.Top){Icon(Icons.Default.Notifications,"Notification",tint=Red,modifier=Modifier.padding(end=12.dp));Column{Text(item.title?:item.type?.replace('_',' ')?:"Audience Verdict update",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color.White);Text(item.message?:item.body.orEmpty(),Modifier.padding(top=5.dp),fontSize=12.sp,lineHeight=17.sp,color=Color(0xFFD0D5D9));item.createdAt?.let{Text(it.replace('T',' ').take(16),Modifier.padding(top=7.dp),fontSize=10.sp,color=Muted)}}}}}
    }
}

@Composable private fun WaitingListScreenCorrect(nav:androidx.navigation.NavHostController){
    var entries by remember{mutableStateOf<List<WaitingListEntry>>(emptyList())}
    var error by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf("")}
    var refresh by remember{mutableIntStateOf(0)}
    var showDetails by remember{mutableStateOf<Map<String,Show>>(emptyMap())}
    var movieTitles by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    var theatreNames by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    val context=LocalContext.current
    val scope= rememberCoroutineScope()
    suspend fun loadEntries(enrich:Boolean){
        val latest=AvClient.api.waitingList()
        entries=latest
        if(enrich&&latest.isNotEmpty()){
            val loadedShows=latest.map{it.showId}.distinct().mapNotNull{id->runCatching{AvClient.api.show(id)}.getOrNull()?.let{id to it}}.toMap()
            showDetails=loadedShows
            movieTitles=loadedShows.values.map{it.movieId}.filter{it.isNotBlank()}.distinct().mapNotNull{id->runCatching{AvClient.api.movie(id)}.getOrNull()?.let{id to it.title}}.toMap()
            theatreNames=loadedShows.values.map{it.theatreId}.filter{it.isNotBlank()}.distinct().mapNotNull{id->runCatching{AvClient.api.theatre(id)}.getOrNull()?.let{id to it.name}}.toMap()
        }
    }
    LaunchedEffect(refresh){loading=true;error="";try{loadEntries(true)}catch(t:Throwable){error=apiErrorMessage(t,context)}finally{loading=false}}
    LaunchedEffect(Unit){while(isActive){delay(10000);try{loadEntries(false)}catch(_:Throwable){}}}
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text("Waiting List",Modifier.weight(1f),fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);TextButton(onClick={refresh++}){Text("Refresh",color=Red)}}
        Text("Track your place and claim an offer before it expires.",Modifier.padding(top=4.dp,bottom=12.dp),fontSize=12.sp,color=Muted)
        when{
            loading->Busy()
            error.isNotBlank()->ErrorCard(error)
            entries.isEmpty()->Text("You haven't joined a show waiting list yet.",Modifier.padding(16.dp),color=Muted)
            else->entries.forEach{entry->
                val status=entry.status.uppercase()
                val active=status in setOf("WAITING","OFFERED")
                val offered=status=="OFFERED"
                val expired=offered&&waitingOfferExpired(entry)
                val show=showDetails[entry.showId]
                val dateTime=listOfNotNull(show?.date?.let(::displayMovieDate),show?.startTime).joinToString(" · ")
                Column(Modifier.fillMaxWidth().padding(vertical=5.dp).border(1.dp,if(offered&&!expired)Red else Color(0xFF343A40),RoundedCornerShape(14.dp)).background(Panel,RoundedCornerShape(14.dp)).padding(14.dp)){
                    Text(movieTitles[show?.movieId]?:"Movie show",fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Text(buildString{append(theatreNames[show?.theatreId]?:"Show ${entry.showId}");if(dateTime.isNotBlank())append(" · $dateTime")},Modifier.padding(top=4.dp),fontSize=12.sp,color=Muted)
                    Text("${entry.requestedSeatsCount} ticket${if(entry.requestedSeatsCount==1)"" else "s"} · ${entry.status}",Modifier.padding(top=6.dp),fontSize=12.sp,color=if(offered&&!expired)Color(0xFF50D897) else Color(0xFFD4D8DC))
                    if(status=="WAITING")entry.position?.let{Text("Queue position: $it",Modifier.padding(top=3.dp),fontSize=11.sp,color=Muted)}
                    if(offered)entry.offerExpiresAt?.let{Text(if(expired)"Offer expired" else "Offer expires ${displayTimestamp(it)}",Modifier.padding(top=3.dp),fontSize=11.sp,color=if(expired)Red else Muted)}
                    entry.createdAt?.let{Text("Joined ${displayTimestamp(it)}",Modifier.padding(top=3.dp),fontSize=10.sp,color=Muted)}
                    if(active)Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        if(offered&&!expired)Button(onClick={scope.launch{busyId=entry.id;error="";try{val claimed=AvClient.api.claimWaitingList(entry.id);loadEntries(true);if(claimed.status.equals("CONVERTED",true))nav.navigate("bookings"){launchSingleTop=true}}catch(t:Throwable){error=apiErrorMessage(t,context)}finally{busyId=""}}},Modifier.weight(1f),enabled=busyId!=entry.id,colors=ButtonDefaults.buttonColors(containerColor=Red)){Text(if(busyId==entry.id)"Claiming..." else "Claim offer",color=Color.White)}
                        OutlinedButton(onClick={scope.launch{busyId=entry.id;error="";try{AvClient.api.cancelWaitingList(entry.id);entries=entries.filterNot{it.id==entry.id}}catch(t:Throwable){error=apiErrorMessage(t,context)}finally{busyId=""}}},enabled=busyId!=entry.id,border=BorderStroke(1.dp,Color(0xFF596168))){Text("Leave list",color=Color.White)}
                    }
                }
            }
        }
        if(error.isNotBlank()&&!loading&&entries.isNotEmpty())ErrorCard(error)
    }
}

@Composable private fun MyReviewsScreen(){
    var reviews by remember{mutableStateOf<List<Review>>(emptyList())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")}
    LaunchedEffect(Unit){try{val me=AvClient.api.me();reviews=AvClient.api.reviews().filter{(!me.id.isNullOrBlank()&&it.userId==me.id)||(!me.name.isNullOrBlank()&&it.author.equals(me.name,true))}}catch(t:Throwable){error=t.message?:"Your reviews could not be loaded"}finally{loading=false}}
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)){PageHeader("My Reviews"){};when{loading->Busy();error.isNotBlank()->ErrorCard(error);reviews.isEmpty()->Text("Your published reviews will appear here.",Modifier.padding(16.dp),fontSize=13.sp,color=Muted);else->reviews.forEach{review->Column(Modifier.fillMaxWidth().padding(vertical=5.dp).background(Panel,RoundedCornerShape(13.dp)).padding(14.dp)){Text(review.title?:"Your review",fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("${"★".repeat((review.rating?:0).coerceIn(0,5))}  ${review.status?:"Published"}",Modifier.padding(top=5.dp),fontSize=12.sp,color=Color(0xFFFFC45C));Text(review.text.orEmpty(),Modifier.padding(top=6.dp),fontSize=12.sp,lineHeight=17.sp,color=Color(0xFFD0D5D9));review.createdAt?.let{Text(it.replace('T',' ').take(16),Modifier.padding(top=6.dp),fontSize=10.sp,color=Muted)}}}}
}
}

@Composable private fun HelpSupportScreen(){
    val faqs=listOf("How do I book tickets?" to "Choose a movie and showtime, select available seats, review the booking conditions, and confirm. Your booking and ticket reference appear in My Bookings after the server confirms it.","Where can I find my ticket?" to "Open Profile → My Bookings and select a confirmed booking to view the ticket and its QR code.","Why can't I select some seats?" to "Seats can be unavailable because another customer booked, reserved, or temporarily held them. Refresh the show and try another available seat.","Can I cancel a booking?" to "Open My Bookings and choose the booking. Cancellation availability and any refund depend on the show and cinema policy.","How do I join a waiting list?" to "When a show is sold out, use Join Waiting List on its seat page. If the backend offers you seats, open the entry and claim before its expiry.","I did not receive a booking update" to "Check the in-app Notifications page and confirm that your account contact details are current. If the booking is missing, contact support with your booking reference.")
    var expanded by remember{mutableIntStateOf(-1)};val context=LocalContext.current
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(16.dp)){PageHeader("Help & Support"){};Text("We're here to help",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("Find answers or contact Audience Verdict support.",Modifier.padding(top=5.dp,bottom=15.dp),fontSize=13.sp,color=Muted);Column(Modifier.border(1.dp,Color(0xFF30383D),RoundedCornerShape(13.dp)).clip(RoundedCornerShape(13.dp))){faqs.forEachIndexed{i,pair->AboutAccordionRow(pair.first,pair.second,"?",expanded==i,{expanded=if(expanded==i)-1 else i})}};Text("Contact us",Modifier.padding(top=20.dp),fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("Email support",Modifier.padding(top=8.dp),fontSize=12.sp,color=Muted);Text("support@audienceverdict.in",Modifier.padding(top=4.dp).clickable{val intent=Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:support@audienceverdict.in"));intent.putExtra(Intent.EXTRA_SUBJECT,"Audience Verdict support");runCatching{context.startActivity(intent)}},fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Red);Text("Include your booking reference and a short description of the issue so we can help faster.",Modifier.padding(top=5.dp,bottom=20.dp),fontSize=12.sp,lineHeight=17.sp,color=Muted)}
}

@Composable private fun EditProfileScreen(onSaved:()->Unit){
    val context=LocalContext.current
    var name by remember{mutableStateOf("")}
    var email by remember{mutableStateOf("")}
    var mobile by remember{mutableStateOf("")}
    var profileMobileVerified by remember{mutableStateOf(false)}
    var alternativeMobile by remember{mutableStateOf("")}
    var profileAlternativeVerified by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    val scope= rememberCoroutineScope()
    LaunchedEffect(Unit){
        try {
            val profile=AvClient.api.me()
            name=profile.name.orEmpty()
            email=profile.email.orEmpty()
            mobile=profile.mobile.orEmpty()
            profileMobileVerified=profile.mobileVerified!=false&&profile.mobile.isNullOrBlank().not()
            alternativeMobile=profile.alternativeMobile.orEmpty()
            profileAlternativeVerified=profile.alternativeMobileVerified==true
        } catch(t:Throwable) { message=apiErrorMessage(t,context) }
    }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageHeader("Edit Profile",onSaved)
        Text("Update your personal details",fontSize=14.sp,color=Muted)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Name")},singleLine=true,colors=fieldColors())
        OutlinedTextField(email,{email=it},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("Email")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),colors=fieldColors())
        OutlinedTextField(mobile,{},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("Mobile number")},placeholder={Text("Verified account mobile")},singleLine=true,readOnly=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),colors=fieldColors())
        Text(if(mobile.isBlank())"No verified mobile number is available." else if(profileMobileVerified)"Mobile number verified." else "Mobile number not verified.",Modifier.padding(start=4.dp,top=4.dp),fontSize=11.sp,color=if(profileMobileVerified)Color(0xFF42D392) else Muted)
        OutlinedTextField(alternativeMobile,{alternativeMobile=it.filter{c->c.isDigit()||c=='+'||c==' '||c=='-'}.take(18)},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("Alternative mobile (optional)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),colors=fieldColors())
        Text(if(alternativeMobile.isBlank())"No alternative mobile is saved." else if(profileAlternativeVerified)"Alternative mobile verified." else "The current API does not provide an alternative mobile verification flow; this number is saved but not verified.",Modifier.padding(start=4.dp,top=4.dp),fontSize=11.sp,color=if(profileAlternativeVerified)Color(0xFF42D392) else Muted)
        if(message.isNotBlank())Text(message,Modifier.padding(top=10.dp),color=if(message.startsWith("Saved"))Color(0xFF42D392) else Red,fontSize=12.sp)
        Button(onClick={scope.launch{
            busy=true;message=""
            try {
                AvClient.api.updateMe(UpdateProfilePayload(name.trim(),email.trim(),alternativeMobile.trim().ifBlank{null}))
                message="Saved"
                onSaved()
            } catch(t:Throwable) { message=apiErrorMessage(t,context) }
            finally { busy=false }
        }},Modifier.fillMaxWidth().padding(top=15.dp),enabled=name.isNotBlank()&&email.contains("@")&&!busy,colors=ButtonDefaults.buttonColors(containerColor=Red)) {
            if(busy)CircularProgressIndicator(Modifier.size(18.dp),color=Color.White) else Text("Save Changes",fontWeight=FontWeight.Bold)
        }
    }
}

@Composable private fun SimpleAccountPage(title:String,body:String){Column(Modifier.fillMaxSize().background(Ink).padding(18.dp)){PageHeader(title){};Text(body,color=Muted,fontSize=14.sp,lineHeight=20.sp)}}

private data class AboutPolicy(val title:String,val detail:String,val icon:String)

@Composable private fun AboutScreen(nav:androidx.navigation.NavHostController) {
    val policies=listOf(
        AboutPolicy("Booking Rules","Tickets are booked through Audience Verdict subject to the selected cinema's availability and rules. Review your show, seats, and confirmation details before completing a booking.","▣"),
        AboutPolicy("Cancellation & Refund","Cancellation eligibility, timing, and refunds depend on the applicable cinema and show policy. Check the booking details and the policy shown for your show.","₹"),
        AboutPolicy("User Responsibilities","Keep your account and contact details accurate, protect your booking reference, and follow instructions from the cinema team.","♙"),
        AboutPolicy("Prohibited Activities","Do not misuse the service, interfere with other customers' bookings, or submit content that is unlawful or misleading.","⊘"),
        AboutPolicy("Privacy Policy","Audience Verdict uses account and booking information to operate the service and support your bookings. Handle personal information responsibly.","◇"),
        AboutPolicy("Terms & Conditions","Use of Audience Verdict is subject to the terms presented in the app. Your selected cinema may also have additional conditions.","▤"),
        AboutPolicy("Content Guidelines","Reviews should reflect your genuine experience and should be respectful, relevant, and lawful.","ⓘ")
    )
    val faqs=listOf(
        "How can I release my movie in Audience Verdict?" to "Use the creator enquiry option to share your film and contact details with the Audience Verdict team.",
        "What types of movies are accepted?" to "Share your film information with the team for review. Listing availability depends on content review and participating venues.",
        "How does the ticket booking process work?" to "Choose a movie, show, and available seats, review the booking details and conditions, then confirm your booking.",
        "How are reviews verified?" to "Reviews are associated with Audience Verdict accounts and may be checked against relevant booking information."
    )
    var expandedPolicy by remember { mutableIntStateOf(0) }
    var expandedFaq by remember { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(bottom=20.dp)) {
        Row(Modifier.fillMaxWidth().padding(start=6.dp,end=16.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={nav.popBackStack()}) { Icon(Icons.Default.ArrowBack,"Back",tint=Color.White) }
            Image(painterResource(R.drawable.av_logo),"Audience Verdict",Modifier.width(158.dp).height(44.dp),contentScale=ContentScale.Fit)
        }
        Box(Modifier.fillMaxWidth().height(260.dp).padding(horizontal=12.dp).clip(RoundedCornerShape(18.dp))) {
            Image(painterResource(R.drawable.about_audience_hero),"Audience watching a film in a cinema",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF2080A0B),Color(0xC9080A0B),Color(0x14080A0B)))))
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.84f).padding(18.dp)) {
                Text("ABOUT AUDIENCE VERDICT",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.8.sp,color=Red)
                Text("Great movies\nstart conversations.",Modifier.padding(top=8.dp),fontSize=31.sp,lineHeight=33.sp,fontWeight=FontWeight.Black,color=Color.White)
                Text("Audience Verdict is a platform where movies meet real audiences.",Modifier.padding(top=8.dp),fontSize=14.sp,lineHeight=19.sp,color=Color(0xFFE0E3E6))
                Row(Modifier.padding(top=13.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={nav.navigate("movies"){launchSingleTop=true}},colors=ButtonDefaults.buttonColors(containerColor=Red),contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp),shape=RoundedCornerShape(10.dp)){Text("Explore Movies  →",fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.White)}
                    OutlinedButton(onClick={nav.navigate("what"){launchSingleTop=true}},border=BorderStroke(1.dp,Color.White.copy(alpha=.75f)),contentPadding=PaddingValues(horizontal=11.dp,vertical=8.dp),shape=RoundedCornerShape(10.dp)){Text("▷  Watch How It Works",fontSize=10.sp,fontWeight=FontWeight.Bold,color=Color.White)}
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf(Triple("▣","Discover","Find and book tickets for the latest movies."),Triple("★","Share Reviews","Share your honest opinions and help others."),Triple("♧","Movie Community","Join a community of real movie lovers."),Triple("▥","Support Filmmakers","Help good cinema reach more audiences.")).forEach { (symbol,heading,description) ->
                Column(Modifier.weight(1f).defaultMinSize(minHeight=132.dp).border(1.dp,Color(0xFF30383D),RoundedCornerShape(12.dp)).background(Panel,RoundedCornerShape(12.dp)).padding(9.dp)) {
                    Box(Modifier.size(34.dp).border(1.dp,Red.copy(alpha=.65f),RoundedCornerShape(10.dp)).background(Color(0xFF281012),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text(symbol,color=Red,fontSize=19.sp,fontWeight=FontWeight.Bold)}
                    Text(heading,Modifier.padding(top=6.dp),fontSize=10.sp,lineHeight=12.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Text(description,Modifier.padding(top=5.dp),fontSize=9.sp,lineHeight=12.sp,color=Color(0xFFCAD0D4))
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal=12.dp).height(174.dp).clip(RoundedCornerShape(14.dp)).border(1.dp,Red.copy(alpha=.8f),RoundedCornerShape(14.dp))) {
            Image(painterResource(R.drawable.about_filmmaker_cta),"Film production camera on a movie set",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xEE120809),Color(0xC8100909),Color(0x22000000)))))
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.82f).padding(15.dp)) {
                Text("FOR CREATORS & PRODUCERS",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp,color=Red)
                Text("Release your movie\nin Audience Verdict?",Modifier.padding(top=5.dp),fontSize=22.sp,lineHeight=24.sp,fontWeight=FontWeight.Black,color=Color.White)
                Text("Bring your story to the audience with visibility, ticket bookings, real reviews, and community engagement.",Modifier.padding(top=5.dp),fontSize=10.sp,lineHeight=14.sp,color=Color(0xFFE0E3E6))
                Text("Start the Process  →",Modifier.padding(top=8.dp).clickable{nav.navigate("what"){launchSingleTop=true}},fontSize=11.sp,fontWeight=FontWeight.Bold,color=Red)
            }
        }
        Text("HOW IT WORKS",Modifier.padding(start=16.dp,top=18.dp),fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.8.sp,color=Red)
        Text("Our Process",Modifier.padding(start=16.dp,top=2.dp,bottom=9.dp),fontSize=25.sp,fontWeight=FontWeight.Black,color=Color.White)
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Submit Your Movie" to "Share your movie details, posters, trailer, and more.","Review & Verification" to "Our team reviews your content for quality and guidelines.","Get Listed on AV" to "Your movie can be listed with showtimes across partner cinemas.","Reach Audiences & Collect Reviews" to "Get audience views, ratings, and meaningful feedback.").forEachIndexed { index, step ->
                Column(Modifier.weight(1f).defaultMinSize(minHeight=151.dp).border(1.dp,Color(0xFF30383D),RoundedCornerShape(10.dp)).background(Panel,RoundedCornerShape(10.dp)).padding(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(23.dp).background(Red,CircleShape),contentAlignment=Alignment.Center){Text("${index+1}",fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.White)};Text(if(index<2) " ✓" else " →",Modifier.padding(start=4.dp),fontSize=13.sp,color=Red)}
                    Text(step.first,Modifier.padding(top=8.dp),fontSize=10.sp,lineHeight=13.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Text(step.second,Modifier.padding(top=5.dp),fontSize=8.sp,lineHeight=11.sp,color=Color(0xFFCAD0D4))
                }
            }
        }
        Text("RULES & POLICIES",Modifier.padding(start=16.dp,top=19.dp),fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.8.sp,color=Red)
        Text("Important Information",Modifier.padding(start=16.dp,top=2.dp,bottom=9.dp),fontSize=25.sp,fontWeight=FontWeight.Black,color=Color.White)
        Column(Modifier.fillMaxWidth().padding(horizontal=12.dp).border(1.dp,Color(0xFF30383D),RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
            policies.forEachIndexed { index, policy -> AboutAccordionRow(policy.title,policy.detail,policy.icon,expandedPolicy==index,{expandedPolicy=if(expandedPolicy==index)-1 else index}) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp).padding(top=18.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top) {
            Column(Modifier.weight(.9f)) {
                Text("FREQUENTLY ASKED QUESTIONS",fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp,color=Red)
                Text("Got questions?",Modifier.padding(top=4.dp),fontSize=23.sp,lineHeight=25.sp,fontWeight=FontWeight.Black,color=Color.White)
                Text("Find answers to common questions about our platform, booking process and more.",Modifier.padding(top=6.dp),fontSize=10.sp,lineHeight=14.sp,color=Color(0xFFCAD0D4))
                Text("View All FAQs  →",Modifier.padding(top=9.dp).border(1.dp,Red,RoundedCornerShape(8.dp)).clickable{expandedFaq=0}.padding(horizontal=9.dp,vertical=7.dp),fontSize=10.sp,fontWeight=FontWeight.Bold,color=Color.White)
            }
            Column(Modifier.weight(1.1f).border(1.dp,Color(0xFF30383D),RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))) {
                faqs.forEachIndexed { index, faq -> AboutAccordionRow(faq.first,faq.second,"",expandedFaq==index,{expandedFaq=if(expandedFaq==index)-1 else index},compact=true) }
            }
        }
    }
}

@Composable private fun AboutAccordionRow(title:String,detail:String,icon:String,expanded:Boolean,onClick:()->Unit,compact:Boolean=false) {
    Column(Modifier.fillMaxWidth().background(if(expanded) Color(0xFF1E1113) else Color.Transparent)) {
        Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=if(compact) 9.dp else 12.dp,vertical=if(compact) 8.dp else 10.dp),verticalAlignment=Alignment.CenterVertically) {
            if(icon.isNotBlank()) Text(icon,Modifier.width(28.dp),fontSize=17.sp,color=if(expanded) Red else Color(0xFFE2E5E8))
            Text(title,Modifier.weight(1f),fontSize=if(compact) 9.sp else 11.sp,fontWeight=if(expanded) FontWeight.SemiBold else FontWeight.Medium,color=Color.White,maxLines=if(compact) 2 else 1)
            Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,if(expanded) "Collapse" else "Expand",Modifier.size(if(compact) 17.dp else 20.dp),tint=if(expanded) Red else Color(0xFFD7DBDF))
        }
        if(expanded) Text(detail,Modifier.fillMaxWidth().padding(start=if(compact) 9.dp else 40.dp,end=12.dp,bottom=10.dp),fontSize=if(compact) 9.sp else 10.sp,lineHeight=15.sp,color=Color(0xFFD0D5D9))
        HorizontalDivider(color=Color(0xFF293035),thickness=.6.dp)
    }
}

private data class OperatingSystemSection(val index:String,val eyebrow:String,val title:String,val body:String,val action:String,val image:Int)

@Composable private fun WhatIsAVScreen(nav:androidx.navigation.NavHostController) {
    val context=LocalContext.current
    var videoView by remember { mutableStateOf<android.widget.VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { videoView?.stopPlayback();videoView=null } }
    val sections=listOf(
        OperatingSystemSection("01","SYSTEM OVERVIEW","The Audience Verdict operating system, at a glance","A single view of the AAV model, its principles, campaign stages, and measurable outcomes.","Explore the framework",R.drawable.av_os_page_01),
        OperatingSystemSection("02","THE FRAMEWORK","A documented system for audience led film success","The operating model and execution lifecycle for consistent, verifiable audience feedback.","Explore the framework",R.drawable.av_os_overview),
        OperatingSystemSection("03","KEY INSIGHTS","A connected system, from promotion to insight","How the campaign workflow moves from planning and registration to screening, verdict, and business outcomes.","See how the system connects",R.drawable.av_os_page_03),
        OperatingSystemSection("04","PRINCIPLES & PEOPLE","Built on audience first principles","Transparency, authenticity, respect, and accountability connect audiences, venues, and film teams.","Explore the principles",R.drawable.av_os_page_05),
        OperatingSystemSection("05","EXECUTION","Clear roles. A six stage engine.","Defined responsibilities, artefacts, and measures keep each stage accountable.","View the execution model",R.drawable.av_os_page_07),
        OperatingSystemSection("06","CAMPAIGN FLOW","From first awareness to a verified verdict","The funnel builds awareness, captures registrations, and provides verified results for business decisions.","Follow the campaign stages",R.drawable.av_os_page_08),
        OperatingSystemSection("07","OPERATIONS","Repeatable operations, ready to use artefacts","Standard procedures and practical tools help teams act consistently at every touchpoint.","Review the operating tools",R.drawable.av_os_page_11),
        OperatingSystemSection("08","MEASUREMENT","A cadence that leads to useful intelligence","Planned measurement captures registration trends, attendance, sentiment, and verdict metrics.","See the measurement approach",R.drawable.av_os_page_13),
        OperatingSystemSection("09","WHAT COMES NEXT","Designed to grow beyond a single campaign","The roadmap describes near term infrastructure, multi tenant support, and long term integrations.","Start a conversation",R.drawable.av_os_page_15),
        OperatingSystemSection("10","THE PROMOTIONAL PARADIGM","A more authentic way to bring films to audiences","Audience led discovery and real feedback complement traditional promotion.","Explore the model",R.drawable.av_os_page_02),
        OperatingSystemSection("11","CULTURAL SOFTWARE","Principles that shape every interaction","The system is built around transparency, accountability, courage, and respect.","Read the principles",R.drawable.av_os_page_04),
        OperatingSystemSection("12","ROLE DESIGN","A clear blueprint for every role","Responsibilities, requirements, and daily activities make the audience experience repeatable.","Explore role design",R.drawable.av_os_page_06),
        OperatingSystemSection("13","TRANSACTION","From screening to business insight","Verified audience sentiment helps teams understand what connects with viewers.","See the transaction stages",R.drawable.av_os_page_09),
        OperatingSystemSection("14","RESILIENCE","Standard procedures for dependable campaigns","Predefined responses help teams handle theatre, technology, publicity, and people risks.","Review resilience practices",R.drawable.av_os_page_10),
        OperatingSystemSection("15","CONTENT CADENCE","The right message at the right moment","A planned communication timeline keeps audiences informed from discovery through the verdict.","Explore the cadence",R.drawable.av_os_page_12),
        OperatingSystemSection("16","LONG TERM ROADMAP","An ecosystem designed to evolve","A practical path connects the current model with future tools and integrations.","View the roadmap",R.drawable.av_os_page_14)
    )
    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start=8.dp,end=16.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={nav.popBackStack()}){Icon(Icons.Default.ArrowBack,"Back",tint=Color.White)}
                Image(painterResource(R.drawable.av_logo),"Audience Verdict",Modifier.width(150.dp).height(42.dp),contentScale=ContentScale.Fit)
            }
            Column(Modifier.padding(horizontal=16.dp)) {
                Text("WHAT IS AV?",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp,color=Red)
                Text("What is\nAudience Verdict?",Modifier.padding(top=4.dp),fontSize=31.sp,lineHeight=34.sp,fontWeight=FontWeight.Black,color=Color.White)
                Text("A framework for audience led film success.",Modifier.padding(top=5.dp),fontSize=14.sp,color=Color(0xFFE2E5E8))
                Box(Modifier.fillMaxWidth().padding(top=14.dp).height(205.dp).clip(RoundedCornerShape(17.dp)).border(1.dp,Red.copy(alpha=.8f),RoundedCornerShape(17.dp))) {
                    Image(painterResource(R.drawable.what_is_av_hero),"Cinematic theatre hero background",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    AndroidView(factory={ctx->android.widget.VideoView(ctx).apply{
                        videoView=this
                        setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${R.raw.what_is_av_hero}"))
                        setOnPreparedListener{player->player.isLooping=true;isPrepared=true}
                        setOnCompletionListener{isPlaying=false}
                        setOnErrorListener{_,_,_->isPlaying=false;true}
                    }},modifier=Modifier.fillMaxSize(),update={videoView=it})
                    if(!isPlaying) {
                        Image(painterResource(R.drawable.what_is_av_hero),"",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xCC050607),Color(0x88050708),Color(0x22050708)))))
                        Column(Modifier.align(Alignment.CenterStart).padding(horizontal=17.dp,vertical=12.dp).fillMaxWidth(.79f)) {
                            Text("01 / FEATURED FILM",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp,color=Red)
                            Text("The AAV Story",Modifier.padding(top=7.dp),fontSize=27.sp,lineHeight=30.sp,fontWeight=FontWeight.Black,color=Color.White)
                            Text("Discover the philosophy, operating model, and execution framework behind Audience Verdict.",Modifier.padding(top=5.dp),fontSize=12.sp,lineHeight=16.sp,color=Color(0xFFE4E7E9))
                            Text("Explore the framework  ›",Modifier.padding(top=8.dp).clickable{nav.navigate("about")},fontSize=11.sp,fontWeight=FontWeight.Bold,color=Red)
                        }
                        IconButton(onClick={if(isPrepared){videoView?.start();isPlaying=true}},modifier=Modifier.align(Alignment.BottomEnd).padding(10.dp).size(42.dp).background(Red,CircleShape),enabled=isPrepared) {
                            Icon(Icons.Default.PlayArrow,"Play the AAV story",tint=Color.White,modifier=Modifier.size(26.dp))
                        }
                    } else {
                        IconButton(onClick={videoView?.pause();isPlaying=false},modifier=Modifier.align(Alignment.TopEnd).padding(8.dp).size(38.dp).background(Color(0x99050708),CircleShape)) {
                            Icon(Icons.Default.Pause,"Pause video and show title",tint=Color.White)
                        }
                        IconButton(onClick={videoView?.pause();videoView?.seekTo(0);isPlaying=false},modifier=Modifier.align(Alignment.BottomEnd).padding(8.dp).size(38.dp).background(Color(0x99050708),CircleShape)) {
                            Icon(Icons.Default.Stop,"Stop video and show title",tint=Color.White)
                        }
                    }
                    if(!isPrepared&&!isPlaying) Text("Loading video…",Modifier.align(Alignment.BottomStart).padding(12.dp),fontSize=9.sp,color=Color.White.copy(alpha=.8f))
                }
                Text("THE AUDIENCE VERDICT OPERATING SYSTEM",Modifier.padding(top=22.dp,bottom=9.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp,color=Red)
            }
            sections.forEach { section ->
                val imageOnLeft=(section.index.toIntOrNull()?:1)%2==1
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=5.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF0D1114)).border(1.dp,Color(0xFF1D252A),RoundedCornerShape(13.dp)).padding(9.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(imageOnLeft) Image(painterResource(section.image),section.title,Modifier.weight(.92f).height(105.dp).clip(RoundedCornerShape(9.dp)).background(Color.White),contentScale=ContentScale.Crop)
                    Column(Modifier.weight(1.08f).padding(horizontal=if(imageOnLeft) 11.dp else 0.dp)) {
                        Text("${section.index} / ${section.eyebrow}",fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=.7.sp,color=Red,maxLines=1)
                        Text(section.title,Modifier.padding(top=4.dp),fontSize=14.sp,lineHeight=17.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=2)
                        Text(section.body,Modifier.padding(top=4.dp),fontSize=10.sp,lineHeight=13.sp,color=Color(0xFFC8CED3),maxLines=3)
                        Text("${section.action}  ›",Modifier.padding(top=5.dp).clickable{nav.navigate("about")},fontSize=9.sp,fontWeight=FontWeight.Bold,color=Red,maxLines=1)
                    }
                    if(!imageOnLeft) Image(painterResource(section.image),section.title,Modifier.weight(.92f).padding(start=11.dp).height(105.dp).clip(RoundedCornerShape(9.dp)).background(Color.White),contentScale=ContentScale.Crop)
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp).height(106.dp).clip(RoundedCornerShape(15.dp)).border(1.dp,Red,RoundedCornerShape(15.dp))) {
                Image(painterResource(R.drawable.showtimes_cinema_bg),"Cinema seats",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE608090A),Color(0xAA08090A),Color(0x2208090A)))))
                Column(Modifier.align(Alignment.CenterStart).padding(14.dp).fillMaxWidth(.75f)) {
                    Text("THE NEXT CHAPTER",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Red)
                    Text("Make every audience voice count.",Modifier.padding(top=4.dp),fontSize=19.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Text("Explore films, meet the audience, and build a stronger conversation around cinema.",Modifier.padding(top=4.dp),fontSize=10.sp,lineHeight=13.sp,color=Color(0xFFD2D6DA))
                    Text("Explore movies  ›",Modifier.padding(top=5.dp).clickable{nav.navigate("movies")},fontSize=10.sp,fontWeight=FontWeight.Bold,color=Red)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable private fun InfoPage(title:String,body:String){Column(Modifier.fillMaxSize().background(Ink).padding(20.dp)){Text(title,fontWeight=FontWeight.Bold,fontSize=24.sp);Spacer(Modifier.height(18.dp));Text(body,color=Muted,lineHeight=25.sp)}}














