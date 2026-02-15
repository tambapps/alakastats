package com.tambapps.pokemon.alakastats.ui.screen.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.screen.home.AlakastatsLabel
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


object AboutScreen : Screen {
    @Composable
    override fun Content() {
        val isCompact = LocalIsCompact.current
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (isCompact) Modifier.safeContentPadding().padding(horizontal = 4.dp)
                    else Modifier.padding(all = 16.dp)
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlakastatsLabel()
            VerticalSpacer(8.dp)
            AboutMe()
            VerticalSpacer()
            AboutAlakastats()
            VerticalSpacer()
            Credits()
            VerticalSpacer()
            val navigator = LocalNavigator.currentOrThrow
            OutlinedButton(onClick = { navigator.pop() }) {
                Text("OK")
            }
        }
    }
}

private val linkStyle = SpanStyle(
    color = Color(0xFF1565C0),
    textDecoration = TextDecoration.Underline
)

@Composable
private fun AboutMe() = Section(
    title = "About me",
    text = buildAnnotatedString {
        append("\t\tI am a french Pokemon VGC player and developer, known by the pseudos ")
        withLink(LinkAnnotation.Url("https://x.com/jarmanVGC")) {
            withStyle(linkStyle) { append("JarMan") }
        }
        append(" and ")
        withLink(LinkAnnotation.Url("https://github.com/tambapps")) {
            withStyle(linkStyle) { append("Tambapps") }
        }
        append(".")
    }

)



@Composable
private fun AboutAlakastats() = Section(
    title = "About Alakastats",
    text = buildAnnotatedString {
        append("\t\tI built Alakastats to have a single place where I can analyze how I use my team, track my decisions, and take structured notes on matchups. ")
        append("My goal was to better understand my habits, identify mistakes, and continuously improve my competitive play.\n\n")
        append("The app ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("does not collect any personal data")
        }
        append(". Everything is stored locally on your device (in your browser or on your smartphone).\n\n")

        append("You can consult the full source code ")
        withLink(LinkAnnotation.Url("https://github.com/tambapps/alakastats")) {
            withStyle(linkStyle) { append("here") }
        }
        append(".")
    }
)

@Composable
private fun Credits() = Section(
    title = "Credits",
    text = buildAnnotatedString {
        append("\t\tSpecial thanks to\n")

        append("- ")
        withLink(LinkAnnotation.Url("https://deviantart.com/jormxdos")) {
            withStyle(linkStyle) { append("jormxdos") }
        }
        append(" to have designed the tera type logos\n")

        append("- ")
        withLink(LinkAnnotation.Url("https://pokeapi.co/")) {
            withStyle(linkStyle) { append("PokeApi") }
        }
        append(" allowing the app to fetch Pokemon-related data to display in the app\n")

        append("- myself to have designed the whole app from scratch")
    }
)

@Composable
private fun Section(title: String, text: AnnotatedString) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.Start))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun VerticalSpacer(height: Dp = 20.dp) = Spacer(Modifier.height(height))