package com.belluxx.transfire

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Aod
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.belluxx.transfire.ui.theme.TransFireTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransFireTheme {
                val activity = LocalActivity.current

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OnboardingContent(
                        modifier = Modifier.padding(innerPadding),
                        onSetupClick = {
                            startActivity(Intent(this@OnboardingActivity, SetupActivity::class.java))
                            activity?.finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingContent(
    modifier: Modifier = Modifier,
    onSetupClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row {
            Icon(
                imageVector = Icons.Default.Aod,
                contentDescription = null,
                modifier = Modifier.size(48.dp).alpha(0.8F)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(48.dp).alpha(0.8F)
            )

            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                modifier = Modifier.size(48.dp).alpha(0.8F),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Privately proxy your LLMs through Firebase",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 1.5.em,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onSetupClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingPreview() {
    TransFireTheme {
        OnboardingContent()
    }
}