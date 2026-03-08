# CSS → Jetpack Compose Mapping Reference

This file maps every CSS class and token in the UI prototype to its Jetpack Compose equivalent for Android implementation.

## Color Tokens

| CSS Variable | Value (Light) | Value (Dark) | Compose Property |
|---|---|---|---|
| `--primary` | `#E85D2C` | `#FFB59C` | `MaterialTheme.colorScheme.primary` |
| `--on-primary` | `#FFFFFF` | `#5F1600` | `MaterialTheme.colorScheme.onPrimary` |
| `--primary-container` | `#FFF0E8` | `#862200` | `MaterialTheme.colorScheme.primaryContainer` |
| `--on-primary-container` | `#3A0A00` | `#FFDBD0` | `MaterialTheme.colorScheme.onPrimaryContainer` |
| `--secondary` | `#4A7A20` | `#A8D475` | `MaterialTheme.colorScheme.secondary` |
| `--on-secondary` | `#FFFFFF` | `#1A3700` | `MaterialTheme.colorScheme.onSecondary` |
| `--secondary-container` | `#C8F09A` | `#2D5000` | `MaterialTheme.colorScheme.secondaryContainer` |
| `--on-secondary-container` | `#0F2000` | `#C8F09A` | `MaterialTheme.colorScheme.onSecondaryContainer` |
| `--tertiary` | `#7A4E22` | `#E6BC8E` | `MaterialTheme.colorScheme.tertiary` |
| `--on-tertiary` | `#FFFFFF` | `#432C0A` | `MaterialTheme.colorScheme.onTertiary` |
| `--tertiary-container` | `#FFDDB8` | `#5D4119` | `MaterialTheme.colorScheme.tertiaryContainer` |
| `--on-tertiary-container` | `#2E1500` | `#FFDDB8` | `MaterialTheme.colorScheme.onTertiaryContainer` |
| `--background` | `#FFF8F2` | `#1C1B1F` | `MaterialTheme.colorScheme.background` |
| `--on-background` | `#1C1B1F` | `#E6E1E5` | `MaterialTheme.colorScheme.onBackground` |
| `--surface` | `#FFFFFF` | `#2B2930` | `MaterialTheme.colorScheme.surface` |
| `--on-surface` | `#1C1B1F` | `#E6E1E5` | `MaterialTheme.colorScheme.onSurface` |
| `--surface-variant` | `#F7EDE3` | `#49454F` | `MaterialTheme.colorScheme.surfaceVariant` |
| `--on-surface-variant` | `#49454F` | `#CAC4D0` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `--surface-warm` | `#FFF5ED` | `#2D2520` | Custom: `SurfaceWarm` in Color.kt |
| `--surface-container` | `#F8F0E8` | `#352E28` | Custom: `SurfaceContainer` in Color.kt |
| `--error` | `#BA1A1A` | `#FFB4AB` | `MaterialTheme.colorScheme.error` |
| `--on-error` | `#FFFFFF` | `#690005` | `MaterialTheme.colorScheme.onError` |
| `--error-container` | `#FFDAD6` | `#93000A` | `MaterialTheme.colorScheme.errorContainer` |
| `--on-error-container` | `#410002` | `#FFDAD6` | `MaterialTheme.colorScheme.onErrorContainer` |
| `--outline` | `#7A757F` | `#938F99` | `MaterialTheme.colorScheme.outline` |
| `--outline-variant` | `#CAC4D0` | `#49454F` | `MaterialTheme.colorScheme.outlineVariant` |
| `--inverse-surface` | `#313033` | `#E6E1E5` | `MaterialTheme.colorScheme.inverseSurface` |
| `--inverse-on-surface` | `#F4EFF4` | `#313033` | `MaterialTheme.colorScheme.inverseOnSurface` |
| `--inverse-primary` | `#FFB59C` | `#E85D2C` | `MaterialTheme.colorScheme.inversePrimary` |
| `--scrim` | `rgba(0,0,0,0.32)` | `rgba(0,0,0,0.5)` | `MaterialTheme.colorScheme.scrim` |

### Dietary Tag Colors (Custom)

| CSS Variable | Value | Compose |
|---|---|---|
| `--diet-vegetarian` | `#5A822B` | `DietVegetarian` in Color.kt |
| `--diet-nonveg` | `#BA1A1A` | `DietNonVeg` |
| `--diet-vegan` | `#2D5000` | `DietVegan` |
| `--diet-jain` | `#E6A817` | `DietJain` |
| `--diet-fasting` | `#7C3AED` | `DietFasting` |
| `--diet-sattvic` | `#6B5B95` | `DietSattvic` |
| `--diet-halal` | `#2E8B57` | `DietHalal` |
| `--diet-eggetarian` | `#D4A574` | `DietEggetarian` |

## Gradient Tokens

| CSS Variable | Light Value | Dark Value | Compose Implementation |
|---|---|---|---|
| `--gradient-hero` | `linear-gradient(165deg, #E85D2C 0%, #F4845F 50%, #FFF0E8 100%)` | `linear-gradient(165deg, #862200 0%, #5F1600 50%, #2D2520 100%)` | `Brush.linearGradient(listOf(Primary, Color(0xFFF4845F), PrimaryContainer), start=Offset(0f,0f), end=Offset(width,height))` |
| `--gradient-warm` | `linear-gradient(135deg, #FFF8F2 0%, #FFF0E8 100%)` | `linear-gradient(135deg, #1C1B1F 0%, #2D2520 100%)` | `Brush.linearGradient(listOf(Background, PrimaryContainer))` |
| `--gradient-card` | `linear-gradient(180deg, #FFFFFF 0%, #FFF8F2 100%)` | `linear-gradient(180deg, #2B2930 0%, #1C1B1F 100%)` | `Brush.verticalGradient(listOf(Surface, Background))` |

## Spacing Tokens

| CSS Variable | Value | Compose |
|---|---|---|
| `--sp-xxs` | `2px` | `2.dp` |
| `--sp-xs` | `4px` | `4.dp` |
| `--sp-sm` | `8px` | `8.dp` |
| `--sp-md` | `16px` | `16.dp` |
| `--sp-lg` | `24px` | `24.dp` |
| `--sp-xl` | `32px` | `32.dp` |
| `--sp-xxl` | `48px` | `48.dp` |
| `--sp-xxxl` | `64px` | `64.dp` |
| `--screen-padding` | `16px` | `16.dp` (horizontal padding) |
| `--card-padding` | `16px` | `16.dp` |
| `--card-gap` | `12px` | `12.dp` |
| `--section-gap` | `24px` | `24.dp` |

## Shape Tokens

| CSS Variable | Value | Compose |
|---|---|---|
| `--radius-xs` | `4px` | `RoundedCornerShape(4.dp)` |
| `--radius-sm` | `8px` | `RoundedCornerShape(8.dp)` |
| `--radius-md` | `16px` | `RoundedCornerShape(16.dp)` |
| `--radius-lg` | `24px` | `RoundedCornerShape(24.dp)` |
| `--radius-xl` | `32px` | `RoundedCornerShape(32.dp)` |
| `--radius-full` | `9999px` | `CircleShape` |

## Shadow Mapping

| CSS Token | Compose | Notes |
|---|---|---|
| `--shadow-sm` | `Modifier.shadow(2.dp, shape)` | Light theme uses warm `rgba(139,90,43,...)` — Compose shadow color is system-controlled, so warm tint comes from surface color, not shadow |
| `--shadow-md` | `Modifier.shadow(4.dp, shape)` | |
| `--shadow-lg` | `Modifier.shadow(8.dp, shape)` | |
| `--shadow-xl` | `Modifier.shadow(16.dp, shape)` | Used for phone frame, FAB, dialogs |

**Note:** Android shadow colors aren't directly controllable in Compose (they use system ambient/key light). The warm shadow effect in CSS is purely cosmetic — on Android, warm tones come from the surface colors and elevation tinting system.

## Card Components

| CSS Class | Compose Implementation |
|---|---|
| `.card-elevated` | `ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp), shape = RoundedCornerShape(16.dp))` |
| `.card-filled` | `Card(colors = CardDefaults.cardColors(containerColor = SurfaceWarm), shape = RoundedCornerShape(16.dp))` |
| `.card-outlined` | `OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp))` |

## Button Components

| CSS Class | Compose Implementation |
|---|---|
| `.btn-filled` | `Button(onClick, shape = CircleShape) { Text(...) }` |
| `.btn-filled.btn-large` | `Button(onClick, modifier = Modifier.height(48.dp).fillMaxWidth(), shape = CircleShape)` |
| `.btn-outlined` | `OutlinedButton(onClick, shape = CircleShape)` |
| `.btn-text` | `TextButton(onClick)` |
| `.btn-tonal` | `FilledTonalButton(onClick, shape = CircleShape)` |
| `.icon-btn` | `IconButton(onClick) { Icon(...) }` |
| `.fab` | `FloatingActionButton(onClick, containerColor = PrimaryContainer)` |

## Chip Components

| CSS Class | Compose Implementation |
|---|---|
| `.chip` | `AssistChip(onClick, label = { Text(...) })` |
| `.chip.chip-selected` | `FilterChip(selected = true, onClick, label = { Text(...) })` |
| `.chip.chip-suggestion` | `SuggestionChip(onClick, label = { Text(...) })` |

## Typography

| CSS Class | Compose Style | Font |
|---|---|---|
| `.display-large` | `MaterialTheme.typography.displayLarge` | Outfit 57/700 |
| `.display-medium` | `MaterialTheme.typography.displayMedium` | Outfit 45/600 |
| `.headline-large` | `MaterialTheme.typography.headlineLarge` | Outfit 32/600 |
| `.headline-medium` | `MaterialTheme.typography.headlineMedium` | Outfit 28/500 |
| `.title-large` | `MaterialTheme.typography.titleLarge` | Outfit 22/500 |
| `.title-medium` | `MaterialTheme.typography.titleMedium` | DM Sans 16/500 |
| `.body-large` | `MaterialTheme.typography.bodyLarge` | DM Sans 16/400 |
| `.body-medium` | `MaterialTheme.typography.bodyMedium` | DM Sans 14/400 |
| `.body-small` | `MaterialTheme.typography.bodySmall` | DM Sans 12/400 |
| `.label-large` | `MaterialTheme.typography.labelLarge` | DM Sans 14/500 |
| `.label-medium` | `MaterialTheme.typography.labelMedium` | DM Sans 12/500 |
| `.section-header-label` | `Text(style = LabelSmall.copy(fontWeight = W600, letterSpacing = 0.8.sp), color = OnSurfaceVariant, text = text.uppercase())` | DM Sans 12/600 uppercase |

## Navigation Components

| CSS Class | Compose Implementation |
|---|---|
| `.bottom-nav` | `NavigationBar { items.forEach { NavigationBarItem(selected, onClick, icon, label) } }` |
| `.bottom-nav .nav-item` | `NavigationBarItem(selected = isSelected, onClick = onClick, icon = { Icon(...) }, label = { Text(...) })` |
| `.top-app-bar` | `TopAppBar(title = { Text(...) }, navigationIcon = { IconButton { Icon(Icons.AutoMirrored.Filled.ArrowBack) } })` |
| `.tab-bar` | `TabRow(selectedTabIndex) { tabs.forEach { Tab(selected, onClick, text = { Text(...) }) } }` |

## Input Components

| CSS Class | Compose Implementation |
|---|---|
| `.text-field` | `OutlinedTextField(value, onValueChange, label = { Text(...) }, shape = RoundedCornerShape(8.dp))` |
| `.search-bar` | `SearchBar(query, onQueryChange, onSearch, active, onActiveChange)` |
| `.switch` + `.switch-track` | `Switch(checked, onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = Primary))` |
| `.progress-bar` | `LinearProgressIndicator(progress, color = Primary, trackColor = SurfaceVariant)` |
| `.progress-circular` | `CircularProgressIndicator(color = Primary)` |

## Animation Mapping

| CSS Animation | Duration | Compose Equivalent |
|---|---|---|
| `fadeInUp` | 300ms | `fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 12 })` |
| `scaleIn` | 200ms | `scaleIn(tween(200), initialScale = 0.95f)` |
| `slideUp` | 300ms | `slideInVertically(tween(300), initialOffsetY = { it })` |
| `bounceIn` | 500ms | `scaleIn(spring(dampingRatio = 0.5f, stiffness = 300f))` |
| `warmPulse` | 600ms | `val scale by animateFloatAsState(if(selected) 1.02f else 1f, tween(600))` then `Modifier.scale(scale)` |
| `shimmer` | 1200ms | `val transition = rememberInfiniteTransition(); val offset by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200)))` then `Brush.linearGradient(colors, startX = offset * width)` |
| `pulse` | 1200ms | `val alpha by transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse))` |
| `stagger-list` (40ms/item) | 350ms each | `LazyColumn { itemsIndexed { i, item -> AnimatedVisibility(visibleState, enter = fadeIn(tween(350, delayMillis = i * 40))) } }` |

### Transition Properties

| CSS Property | Compose Equivalent |
|---|---|
| `transition: all 0.15s ease-out` (`--transition-fast`) | `animateContentSize(tween(150))` or `Modifier.animateColorAsState(tween(150))` |
| `transition: all 0.3s ease-out` (`--transition-normal`) | `animateContentSize(tween(300))` |
| `transition: all 0.5s ease-out` (`--transition-slow`) | `animateContentSize(tween(500))` |

## Reusable Composable Components

| CSS Pattern | Compose Composable | Implementation |
|---|---|---|
| `.empty-state` | `EmptyState(icon: String, title: String, subtitle: String, action: @Composable (() -> Unit)? = null)` | `Column(horizontalAlignment = CenterHorizontally, modifier = Modifier.padding(48.dp)) { Text(icon, fontSize = 48.sp); Text(title, style = TitleMedium, fontWeight = W600); Text(subtitle, style = BodyMedium, color = OnSurfaceVariant) }` |
| `.gradient-header` | `GradientHeader(title: String, subtitle: String? = null)` | `Box(Modifier.background(HeroGradient).padding(24.dp)) { Column { Text(title, fontFamily = Outfit, fontWeight = W600, color = White); subtitle?.let { Text(it, style = BodyMedium, color = White.copy(alpha = 0.85f)) } } }` |
| `.skeleton` / `.skeleton-text` | `ShimmerPlaceholder(modifier: Modifier)` | `Box(modifier.background(shimmerBrush, shape = RoundedCornerShape(4.dp)))` with animated gradient brush |
| `.category-progress` | `CategoryProgress(checked: Int, total: Int)` | `Row { LinearProgressIndicator(progress = checked.toFloat() / total, Modifier.weight(1f)); Text("$checked/$total", style = LabelSmall) }` |
| `.section-header-label` | `SectionHeaderLabel(text: String)` | `Text(text.uppercase(), style = LabelSmall.copy(fontWeight = W600, letterSpacing = 0.8.sp), color = OnSurfaceVariant, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))` |

## Modal/Dialog Components

| CSS Pattern | Compose Implementation |
|---|---|
| `.bottom-sheet` (slideUp) | `ModalBottomSheet(onDismissRequest, sheetState = rememberModalBottomSheetState())` |
| `.dialog` (scaleIn) | `AlertDialog(onDismissRequest, title, text, confirmButton, dismissButton)` or `Dialog { Surface(shape = RoundedCornerShape(24.dp)) { ... } }` |
| `.snackbar` | `SnackbarHost(hostState) { Snackbar(it) }` |

## Meal Card Component

| CSS Structure | Compose |
|---|---|
| `.meal-card` | `ElevatedCard(elevation = 4.dp, shape = RoundedCornerShape(16.dp))` |
| `.meal-card .recipe-image` | `Box(Modifier.size(56.dp).background(WarmGradient, RoundedCornerShape(12.dp)))` |
| `.meal-card .recipe-name` | `Text(name, style = BodyMedium, fontWeight = W600, maxLines = 1, overflow = Ellipsis)` |
| `.meal-card .recipe-meta` | `Row { Text("$time min", style = LabelSmall, color = OnSurfaceVariant); DietaryTag(tag) }` |
| `.meal-card .meal-actions` | `Row { IconButton(onLock) { Icon(if(locked) Filled.Lock else Outlined.Lock) }; IconButton(onSwap) { Icon(Outlined.SwapHoriz) } }` |

## Layout Patterns

| CSS Pattern | Compose |
|---|---|
| `.phone-frame` | N/A — prototype wrapper only |
| `.screen-body` (scrollable content) | `LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp))` |
| `.screen-body` (fixed content) | `Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()))` |
| Day selector band | `LazyRow(modifier = Modifier.background(SurfaceWarm).padding(vertical = 8.dp))` |
| Festival banner | `Surface(color = SurfaceWarm, shape = RoundedCornerShape(12.dp), border = BorderStroke(start = 3.dp, color = Tertiary))` — use `Modifier.drawBehind` for left-only border |

## Scope Toggle (Family/Personal)

| CSS Pattern | Compose |
|---|---|
| `.scope-toggle` | `SegmentedButton(items = listOf("Family", "Personal"), selectedIndex, onSelected)` — use M3 `SingleChoiceSegmentedButtonRow` |
| `.scope-toggle .scope-option.active` | `SegmentedButton(selected = true, colors = SegmentedButtonDefaults.colors(activeContainerColor = PrimaryContainer))` |

---

*Generated from shared.css design tokens. Last updated: March 2026.*
