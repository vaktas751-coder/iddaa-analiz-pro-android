package com.iddaa.analizpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

private val Bg = Color(0xFF0B1220)
private val Panel = Color(0xFF172235)
private val Accent = Color(0xFF38BDF8)
private val Muted = Color(0xFF9FB0C5)

data class FixturesResponse(val response: List<FixtureItem> = emptyList(), val errors: Any? = null)
data class FixtureItem(val fixture: FixtureInfo, val league: LeagueInfo, val teams: TeamsInfo, val goals: GoalsInfo?)
data class FixtureInfo(val date: String = "", val status: StatusInfo? = null)
data class StatusInfo(val short: String = "")
data class LeagueInfo(val name: String = "", val country: String = "")
data class TeamsInfo(val home: TeamInfo, val away: TeamInfo)
data class TeamInfo(val name: String = "")
data class GoalsInfo(val home: Int? = null, val away: Int? = null)

interface FootballApi {
    @GET("fixtures")
    suspend fun fixtures(
        @Header("x-apisports-key") key: String,
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Europe/Istanbul"
    ): FixturesResponse
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

@Composable
fun AppScreen() {
    var matches by remember { mutableStateOf<List<FixtureItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Maçları görmek için Yenile'ye dokunun.") }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Accent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Column(
            Modifier.fillMaxSize().background(Bg).statusBarsPadding().padding(18.dp)
        ) {
            Text("İDDAA ANALİZ PRO", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("AVRUPA LİGLERİ • MAÇ MERKEZİ", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("GÜNÜN MAÇLARI", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("API-Football veri bağlantısı", color = Muted, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            message = "Veriler alınıyor..."
                            try {
                                if (BuildConfig.API_FOOTBALL_KEY.isBlank()) {
                                    message = "API anahtarı henüz eklenmedi. GitHub Secrets bölümüne API_FOOTBALL_KEY ekleyip APK'yı yeniden oluşturun."
                                } else {
                                    val api = Retrofit.Builder()
                                        .baseUrl("https://v3.football.api-sports.io/")
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build().create(FootballApi::class.java)
                                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                        .format(java.util.Date())
                                    matches = api.fixtures(BuildConfig.API_FOOTBALL_KEY, today).response
                                    message = if (matches.isEmpty()) "Bugün için maç bulunamadı." else "${matches.size} maç listelendi."
                                }
                            } catch (e: Exception) {
                                message = "Bağlantı hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                            } finally { loading = false }
                        }
                    },
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
                ) { Text(if (loading) "Yükleniyor" else "Yenile") }
            }
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Text(message, Modifier.padding(14.dp), color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(matches) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("${item.league.name} • ${item.league.country}", color = Accent, fontSize = 11.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("${item.teams.home.name}  -  ${item.teams.away.name}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Skor: ${item.goals?.home ?: "-"} : ${item.goals?.away ?: "-"}   |   ${item.fixture.status?.short ?: ""}", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Not: Bu ilk sürüm maç verilerini listeler. Tahmin yüzdeleri doğrulanmış başarı garantisi değildir; analiz modülü sonraki aşamada eklenecektir.", color = Muted, fontSize = 10.sp)
        }
    }
}
