package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.PortalNavigation
import com.example.ui.navigation.Screen
import com.example.ui.theme.Kalpurush
import com.example.ui.theme.PortalMaroon
import com.example.ui.theme.PortalSaffron

@Composable
fun PortalDrawerContent(
    currentRoute: String,
    isDark: Boolean,
    onNavigate: (String) -> Unit,
    onCategory: (String) -> Unit,
    onYear: (Int) -> Unit,
    onExternal: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onShareApp: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    var onlineOpen by rememberSaveable { mutableStateOf(false) }
    var moreOpen by rememberSaveable { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF111827) else PortalMaroon)
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NingshingCheBrandLogo(size = 46.dp)
                    Column {
                        Text("মাথেল", fontFamily = Kalpurush, color = PortalSaffron, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("নিংশিং চে", fontFamily = Kalpurush, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 22.sp)
                        Text("বিষ্ণুপ্রিয়া মণিপুরি তথ্যকোষ", fontFamily = Kalpurush, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            DrawerRow("ঘর", Icons.Default.Home, currentRoute == Screen.Home.route) {
                onCloseDrawer(); onNavigate(Screen.Home.route)
            }
            DrawerRow("সাম্প্রতিক", Icons.Default.NewReleases, false) {
                onCloseDrawer(); onNavigate(Screen.Home.route)
            }

            DrawerGroup(
                label = "অনলাইন চে",
                expanded = onlineOpen,
                onToggle = { onlineOpen = !onlineOpen }
            ) {
                PortalNavigation.years.forEach { item ->
                    DrawerSubRow(item.label) {
                        onCloseDrawer()
                        item.year?.let(onYear)
                    }
                }
            }

            DrawerRow("ফিচার্ড", Icons.Default.Star, currentRoute == Screen.Featured.route) {
                onCloseDrawer(); onNavigate(Screen.Featured.route)
            }
            DrawerRow("ইমার ঠারর এলা", Icons.Default.MenuBook, currentRoute.contains("language")) {
                onCloseDrawer(); onCategory("language")
            }
            DrawerRow("পৌ", Icons.Default.MenuBook, currentRoute.contains("news")) {
                onCloseDrawer(); onCategory("news")
            }
            DrawerRow("আমার সম্পর্কে", Icons.Default.Info, currentRoute == Screen.About.route) {
                onCloseDrawer(); onNavigate(Screen.About.route)
            }

            DrawerGroup(
                label = "আরাকউ",
                expanded = moreOpen,
                onToggle = { moreOpen = !moreOpen }
            ) {
                DrawerSubRow("লেখক") {
                    onCloseDrawer(); onNavigate(Screen.AuthorsDirectory.route)
                }
                DrawerSubRow("সামাজিক কার্যকলাপ") {
                    onCloseDrawer(); onNavigate(Screen.SocialActivities.route)
                }
                PortalNavigation.moreCategories.forEach { item ->
                    DrawerSubRow(item.label) {
                        onCloseDrawer()
                        item.categorySlug?.let(onCategory)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

            DrawerRow("PDF আর্কাইভ", Icons.Default.PictureAsPdf, currentRoute == Screen.PdfArchive.route) {
                onCloseDrawer(); onNavigate(Screen.PdfArchive.route)
            }
            DrawerRow("লেখা জমাদান", Icons.Default.EditNote, currentRoute == Screen.BlogSubmission.route) {
                onCloseDrawer(); onNavigate(Screen.BlogSubmission.route)
            }
            DrawerRow("অনুসন্ধান", Icons.Default.Search, currentRoute == Screen.Search.route) {
                onCloseDrawer(); onNavigate(Screen.Search.route)
            }
            DrawerRow("সংরক্ষিত", Icons.Default.Bookmark, currentRoute == Screen.Bookmarks.route) {
                onCloseDrawer(); onNavigate(Screen.Bookmarks.route)
            }
            DrawerRow("সেটিংস", Icons.Default.Settings, currentRoute == Screen.Settings.route) {
                onCloseDrawer(); onNavigate(Screen.Settings.route)
            }
            DrawerRow(
                if (isDark) "লাইট থিম" else "ডার্ক থিম",
                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                false
            ) { onToggleTheme() }
            DrawerRow("অ্যাপ শেয়ার করুন", Icons.Default.Share, false) {
                onCloseDrawer(); onShareApp()
            }

            Text(
                text = "কালপুরুষ ফন্ট • নিংশিং চে পোর্টাল",
                fontFamily = Kalpurush,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerGroup(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(240), label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("drawer_group_$label"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = Kalpurush,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = null,
            tint = PortalSaffron,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation)
        )
    }
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(180)) + expandVertically(tween(260)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(220))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fg = if (selected) PortalSaffron else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("drawer_$label"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) PortalSaffron else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            fontFamily = Kalpurush,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
            color = fg
        )
    }
}

@Composable
private fun DrawerSubRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = Kalpurush,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 9.dp)
            .testTag("drawer_sub_$label")
    )
}
