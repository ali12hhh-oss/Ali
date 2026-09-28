package com.velocity.editor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velocity.editor.R
import com.velocity.editor.ui.components.VelocityLogo
import com.velocity.editor.ui.components.VelocityTopBar
import com.velocity.editor.ui.theme.VelocityColors
import com.velocity.editor.util.LanguageManager

@Composable
fun MainScreen(onOpenProject: (Long) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = VelocityColors.Background,
        bottomBar = {
            NavigationBar(containerColor = VelocityColors.Surface) {
                val items = listOf(
                    Triple(R.string.nav_home, Icons.Outlined.Home, 0),
                    Triple(R.string.nav_studio, Icons.Outlined.Movie, 1),
                    Triple(R.string.nav_profile, Icons.Outlined.Person, 2),
                )
                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(label), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VelocityColors.Teal,
                            selectedTextColor = VelocityColors.Teal,
                            unselectedIconColor = VelocityColors.TextSecondary,
                            unselectedTextColor = VelocityColors.TextSecondary,
                            indicatorColor = VelocityColors.SurfaceHigh,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(viewModel, onOpenProject, onOpenProfile = { tab = 2 })
                1 -> StudioScreen(viewModel, onOpenProject)
                else -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun StudioScreen(viewModel: HomeViewModel, onOpenProject: (Long) -> Unit) {
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        VelocityTopBar(leading = { VelocityLogo() })
        if (projects.isEmpty()) {
            Text(
                stringResource(R.string.no_projects), Modifier.padding(16.dp),
                color = VelocityColors.TextSecondary, fontSize = 14.sp,
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(projects, key = { it.id }) { p ->
                Box {
                    ProjectCard(p, Modifier.fillMaxWidth()) { onOpenProject(p.id) }
                    var menu by remember { mutableStateOf(false) }
                    Box(Modifier.align(Alignment.TopEnd)) {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_project)) },
                                onClick = { menu = false; viewModel.deleteProject(p.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    var language by remember { mutableStateOf(LanguageManager.current()) }
    Column(Modifier.fillMaxSize()) {
        VelocityTopBar(leading = { VelocityLogo() })
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.language), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.clip(RoundedCornerShape(14.dp)).background(VelocityColors.Surface)) {
                LanguageRow(R.string.lang_en, language == LanguageManager.EN) {
                    language = LanguageManager.EN; LanguageManager.set(LanguageManager.EN)
                }
                HorizontalDivider(color = VelocityColors.Outline)
                LanguageRow(R.string.lang_ar, language == LanguageManager.AR) {
                    language = LanguageManager.AR; LanguageManager.set(LanguageManager.AR)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.about), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(VelocityColors.Surface).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VelocityLogo(40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("VELOCITY", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(stringResource(R.string.version_value, "1.0.0"), color = VelocityColors.TextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.about_text), color = VelocityColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LanguageRow(label: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = VelocityColors.Teal))
        Text(stringResource(label), fontSize = 15.sp)
    }
}
