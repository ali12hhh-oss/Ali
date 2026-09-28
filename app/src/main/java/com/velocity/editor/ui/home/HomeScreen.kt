package com.velocity.editor.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.velocity.editor.R
import com.velocity.editor.data.ProjectEntity
import com.velocity.editor.ui.components.SectionHeader
import com.velocity.editor.ui.components.VelocityLogo
import com.velocity.editor.ui.components.VelocityTopBar
import com.velocity.editor.ui.theme.VelocityColors
import com.velocity.editor.util.relativeTime

private data class Template(@StringRes val name: Int, val icon: ImageVector, val colors: List<Color>)

private val templates = listOf(
    Template(R.string.tpl_travel, Icons.Outlined.Flight, listOf(Color(0xFF0E7C86), Color(0xFF0B3C5D))),
    Template(R.string.tpl_vlog, Icons.Outlined.Videocam, listOf(Color(0xFFB5651D), Color(0xFF5B2C0E))),
    Template(R.string.tpl_city, Icons.Outlined.LocationCity, listOf(Color(0xFF3B4C7A), Color(0xFF1A1F3A))),
    Template(R.string.tpl_nature, Icons.Outlined.Landscape, listOf(Color(0xFF2E7D4F), Color(0xFF133D26))),
    Template(R.string.tpl_party, Icons.Outlined.Celebration, listOf(Color(0xFF8E3B8E), Color(0xFF3F1745))),
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (Long) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val all by viewModel.allProjects.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    val defaultName = stringResource(R.string.project_default_name, (all.maxOfOrNull { it.id } ?: 0L).toInt() + 1)
    var pendingName by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        val name = pendingName
        pendingName = null
        if (name != null && uris.isNotEmpty()) viewModel.createProject(name, uris, onOpenProject)
    }
    fun startCreate(name: String) {
        pendingName = name
        picker.launch(arrayOf("video/*"))
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            VelocityTopBar(
                leading = { VelocityLogo() },
                trailing = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = VelocityColors.TextPrimary)
                    }
                },
            )
        }
        item { SearchField(query, viewModel::onQueryChange) }
        item { SectionHeader(R.string.my_projects) }
        item {
            if (projects.isEmpty()) {
                Text(
                    stringResource(R.string.no_projects),
                    Modifier.padding(horizontal = 16.dp),
                    color = VelocityColors.TextSecondary, fontSize = 14.sp,
                )
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(projects, key = { it.id }) { p ->
                        ProjectCard(p, Modifier.width(132.dp)) { onOpenProject(p.id) }
                    }
                }
            }
        }
        item { SectionHeader(R.string.create_new) }
        item {
            Box(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(VelocityColors.Teal, VelocityColors.TealDark)))
                    .clickable { startCreate(defaultName) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.new_project), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { SectionHeader(R.string.templates) }
        item {
            val names = templates.map { stringResource(it.name) }
            Row(Modifier.padding(horizontal = 16.dp).height(184.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplateCard(templates[0], Modifier.weight(1f).fillMaxHeight()) { startCreate(names[0]) }
                Column(Modifier.weight(2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemplateCard(templates[1], Modifier.weight(1f).fillMaxHeight()) { startCreate(names[1]) }
                        TemplateCard(templates[2], Modifier.weight(1f).fillMaxHeight()) { startCreate(names[2]) }
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemplateCard(templates[3], Modifier.weight(1f).fillMaxHeight()) { startCreate(names[3]) }
                        TemplateCard(templates[4], Modifier.weight(1f).fillMaxHeight()) { startCreate(names[4]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = VelocityColors.SurfaceHigh,
            unfocusedContainerColor = VelocityColors.SurfaceHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = VelocityColors.Teal,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

@Composable
fun ProjectCard(project: ProjectEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(VelocityColors.Surface).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(84.dp).background(VelocityColors.SurfaceHigh), contentAlignment = Alignment.Center) {
            if (project.thumbnailUri != null) {
                AsyncImage(
                    model = Uri.parse(project.thumbnailUri), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Outlined.Movie, contentDescription = null, tint = VelocityColors.TextSecondary)
            }
        }
        Column(Modifier.padding(8.dp)) {
            Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.last_edited, relativeTime(project.updatedAt)),
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp, color = VelocityColors.Teal,
            )
        }
    }
}

@Composable
private fun TemplateCard(template: Template, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(template.colors))
            .clickable(onClick = onClick).padding(10.dp),
    ) {
        Icon(template.icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.align(Alignment.TopStart).size(22.dp))
        Text(
            stringResource(template.name), Modifier.align(Alignment.BottomStart),
            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        )
    }
}
