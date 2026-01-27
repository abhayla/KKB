package com.rasoiai.app.presentation.onboarding

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.R
import com.rasoiai.app.presentation.splash.components.AppLogo
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpecialDietaryNeed
import com.rasoiai.domain.model.SpiceLevel

@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                OnboardingNavigationEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (uiState.isGenerating) {
        GeneratingScreen(progress = uiState.generatingProgress)
    } else {
        OnboardingContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onBackClick = viewModel::goToPreviousStep,
            onNextClick = viewModel::goToNextStep,
            onHouseholdSizeChange = viewModel::updateHouseholdSize,
            onShowAddMemberDialog = viewModel::showAddMemberDialog,
            onShowEditMemberDialog = viewModel::showEditMemberDialog,
            onDismissMemberDialog = viewModel::dismissMemberDialog,
            onAddOrUpdateMember = viewModel::addOrUpdateFamilyMember,
            onRemoveMember = viewModel::removeFamilyMember,
            onPrimaryDietChange = viewModel::updatePrimaryDiet,
            onToggleDietaryRestriction = viewModel::toggleDietaryRestriction,
            onToggleCuisine = viewModel::toggleCuisine,
            onSpiceLevelChange = viewModel::updateSpiceLevel,
            onToggleDislikedIngredient = viewModel::toggleDislikedIngredient,
            onIngredientSearchQueryChange = viewModel::updateIngredientSearchQuery,
            onAddCustomIngredient = viewModel::addCustomDislikedIngredient,
            onWeekdayCookingTimeChange = viewModel::updateWeekdayCookingTime,
            onWeekendCookingTimeChange = viewModel::updateWeekendCookingTime,
            onToggleBusyDay = viewModel::toggleBusyDay
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    onHouseholdSizeChange: (Int) -> Unit,
    onShowAddMemberDialog: () -> Unit,
    onShowEditMemberDialog: (FamilyMember) -> Unit,
    onDismissMemberDialog: () -> Unit,
    onAddOrUpdateMember: (String, MemberType, Int?, List<SpecialDietaryNeed>) -> Unit,
    onRemoveMember: (FamilyMember) -> Unit,
    onPrimaryDietChange: (PrimaryDiet) -> Unit,
    onToggleDietaryRestriction: (DietaryRestriction) -> Unit,
    onToggleCuisine: (CuisineType) -> Unit,
    onSpiceLevelChange: (SpiceLevel) -> Unit,
    onToggleDislikedIngredient: (String) -> Unit,
    onIngredientSearchQueryChange: (String) -> Unit,
    onAddCustomIngredient: (String) -> Unit,
    onWeekdayCookingTimeChange: (Int) -> Unit,
    onWeekendCookingTimeChange: (Int) -> Unit,
    onToggleBusyDay: (DayOfWeek) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (!uiState.isFirstStep) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                actions = {
                    Text(
                        text = "${uiState.currentStep.stepNumber} of 5",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = spacing.md)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            val animatedProgress by animateFloatAsState(
                targetValue = uiState.progress,
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            // Step content with animation
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState.stepNumber > initialState.stepNumber) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_content",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.HOUSEHOLD_SIZE -> HouseholdSizeStep(
                        householdSize = uiState.householdSize,
                        familyMembers = uiState.familyMembers,
                        onHouseholdSizeChange = onHouseholdSizeChange,
                        onAddMemberClick = onShowAddMemberDialog,
                        onEditMemberClick = onShowEditMemberDialog,
                        onRemoveMemberClick = onRemoveMember
                    )
                    OnboardingStep.DIETARY_PREFERENCES -> DietaryPreferencesStep(
                        primaryDiet = uiState.primaryDiet,
                        dietaryRestrictions = uiState.dietaryRestrictions,
                        onPrimaryDietChange = onPrimaryDietChange,
                        onToggleRestriction = onToggleDietaryRestriction
                    )
                    OnboardingStep.CUISINE_PREFERENCES -> CuisinePreferencesStep(
                        selectedCuisines = uiState.selectedCuisines,
                        spiceLevel = uiState.spiceLevel,
                        onToggleCuisine = onToggleCuisine,
                        onSpiceLevelChange = onSpiceLevelChange
                    )
                    OnboardingStep.DISLIKED_INGREDIENTS -> DislikedIngredientsStep(
                        dislikedIngredients = uiState.dislikedIngredients,
                        searchQuery = uiState.ingredientSearchQuery,
                        onToggleIngredient = onToggleDislikedIngredient,
                        onSearchQueryChange = onIngredientSearchQueryChange,
                        onAddCustomIngredient = onAddCustomIngredient
                    )
                    OnboardingStep.COOKING_TIME -> CookingTimeStep(
                        weekdayCookingTime = uiState.weekdayCookingTimeMinutes,
                        weekendCookingTime = uiState.weekendCookingTimeMinutes,
                        busyDays = uiState.busyDays,
                        onWeekdayTimeChange = onWeekdayCookingTimeChange,
                        onWeekendTimeChange = onWeekendCookingTimeChange,
                        onToggleBusyDay = onToggleBusyDay
                    )
                }
            }

            // Next/Create button
            Button(
                onClick = onNextClick,
                enabled = uiState.canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md)
                    .height(56.dp),
                shape = RoundedCornerShape(spacing.sm)
            ) {
                Text(
                    text = if (uiState.isLastStep) "Create My Meal Plan" else stringResource(R.string.next),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (!uiState.isLastStep) {
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(text = "\u2192")
                }
            }
        }
    }

    // Add/Edit Family Member Dialog
    if (uiState.showAddMemberDialog) {
        FamilyMemberBottomSheet(
            editingMember = uiState.editingMember,
            onDismiss = onDismissMemberDialog,
            onSave = onAddOrUpdateMember
        )
    }
}

// region Step 1: Household Size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseholdSizeStep(
    householdSize: Int,
    familyMembers: List<FamilyMember>,
    onHouseholdSizeChange: (Int) -> Unit,
    onAddMemberClick: () -> Unit,
    onEditMemberClick: (FamilyMember) -> Unit,
    onRemoveMemberClick: (FamilyMember) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md)
    ) {
        Text(
            text = "How many people are\nyou cooking for?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Household size dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "$householdSize ${if (householdSize == 1) "person" else "people"}",
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(spacing.sm)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                (1..10).forEach { size ->
                    DropdownMenuItem(
                        text = { Text("$size ${if (size == 1) "person" else "people"}") },
                        onClick = {
                            onHouseholdSizeChange(size)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Family members section
        Text(
            text = "Family members:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(spacing.md)
        ) {
            Column(modifier = Modifier.padding(spacing.sm)) {
                familyMembers.forEach { member ->
                    FamilyMemberRow(
                        member = member,
                        onEditClick = { onEditMemberClick(member) },
                        onRemoveClick = { onRemoveMemberClick(member) }
                    )
                }

                // Add member button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddMemberClick)
                        .padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = "Add family member",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberRow(
    member: FamilyMember,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            val details = buildString {
                append(member.type.displayName)
                member.age?.let { append(", $it yrs") }
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyMemberBottomSheet(
    editingMember: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (String, MemberType, Int?, List<SpecialDietaryNeed>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(editingMember?.name ?: "") }
    var memberType by remember { mutableStateOf(editingMember?.type ?: MemberType.ADULT) }
    var age by remember { mutableIntStateOf(editingMember?.age ?: 30) }
    val specialNeeds = remember {
        mutableStateListOf<SpecialDietaryNeed>().apply {
            editingMember?.specialNeeds?.let { addAll(it) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md)
        ) {
            Text(
                text = if (editingMember != null) "Edit Family Member" else "Add Family Member",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spacing.sm),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Member type dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            Text(
                text = "Type",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = memberType.displayName,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(spacing.sm)
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    MemberType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                memberType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Age dropdown
            var ageExpanded by remember { mutableStateOf(false) }
            Text(
                text = "Age",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            ExposedDropdownMenuBox(
                expanded = ageExpanded,
                onExpandedChange = { ageExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "$age years",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ageExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(spacing.sm)
                )
                ExposedDropdownMenu(
                    expanded = ageExpanded,
                    onDismissRequest = { ageExpanded = false }
                ) {
                    (1..100).forEach { a ->
                        DropdownMenuItem(
                            text = { Text("$a years") },
                            onClick = {
                                age = a
                                ageExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Special dietary needs
            Text(
                text = "Special dietary needs:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            SpecialDietaryNeed.entries.forEach { need ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (need in specialNeeds) {
                                specialNeeds.remove(need)
                            } else {
                                specialNeeds.add(need)
                            }
                        }
                        .padding(vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = need in specialNeeds,
                        onCheckedChange = {
                            if (it) specialNeeds.add(need) else specialNeeds.remove(need)
                        }
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = need.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name, memberType, age, specialNeeds.toList())
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm)
                ) {
                    Text(if (editingMember != null) "Update" else "Add Member")
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

// endregion

// region Step 2: Dietary Preferences

@Composable
private fun DietaryPreferencesStep(
    primaryDiet: PrimaryDiet,
    dietaryRestrictions: Set<DietaryRestriction>,
    onPrimaryDietChange: (PrimaryDiet) -> Unit,
    onToggleRestriction: (DietaryRestriction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md)
    ) {
        Text(
            text = "What's your primary diet?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Primary diet selection
        Column(modifier = Modifier.selectableGroup()) {
            PrimaryDiet.entries.forEach { diet ->
                DietOptionCard(
                    diet = diet,
                    isSelected = diet == primaryDiet,
                    onClick = { onPrimaryDietChange(diet) }
                )
                Spacer(modifier = Modifier.height(spacing.sm))
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Special dietary restrictions
        Text(
            text = "Special dietary restrictions:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        DietaryRestriction.entries.forEach { restriction ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleRestriction(restriction) }
                    .padding(vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = restriction in dietaryRestrictions,
                    onCheckedChange = { onToggleRestriction(restriction) }
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = restriction.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun DietOptionCard(
    diet: PrimaryDiet,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(spacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = diet.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = diet.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// endregion

// region Step 3: Cuisine Preferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuisinePreferencesStep(
    selectedCuisines: Set<CuisineType>,
    spiceLevel: SpiceLevel,
    onToggleCuisine: (CuisineType) -> Unit,
    onSpiceLevelChange: (SpiceLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md)
    ) {
        Text(
            text = "Which cuisines do you like?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "(Select all that apply)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Cuisine grid (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CuisineCard(
                    cuisine = CuisineType.NORTH,
                    isSelected = CuisineType.NORTH in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.NORTH) },
                    modifier = Modifier.weight(1f),
                    description = "Punjabi, Mughlai"
                )
                CuisineCard(
                    cuisine = CuisineType.SOUTH,
                    isSelected = CuisineType.SOUTH in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.SOUTH) },
                    modifier = Modifier.weight(1f),
                    description = "Tamil, Kerala"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CuisineCard(
                    cuisine = CuisineType.EAST,
                    isSelected = CuisineType.EAST in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.EAST) },
                    modifier = Modifier.weight(1f),
                    description = "Bengali, Odia"
                )
                CuisineCard(
                    cuisine = CuisineType.WEST,
                    isSelected = CuisineType.WEST in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.WEST) },
                    modifier = Modifier.weight(1f),
                    description = "Gujarati, Maharashtrian"
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Spice level dropdown
        Text(
            text = "Spice level:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = spiceLevel.displayName,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(spacing.sm)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SpiceLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.displayName) },
                        onClick = {
                            onSpiceLevelChange(level)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CuisineCard(
    cuisine: CuisineType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(spacing.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (cuisine) {
                    CuisineType.NORTH -> "\uD83E\uDD58"
                    CuisineType.SOUTH -> "\uD83C\uDF5A"
                    CuisineType.EAST -> "\uD83C\uDF5B"
                    CuisineType.WEST -> "\uD83E\uDD57"
                },
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = cuisine.displayName.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// endregion

// region Step 4: Disliked Ingredients

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DislikedIngredientsStep(
    dislikedIngredients: Set<String>,
    searchQuery: String,
    onToggleIngredient: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddCustomIngredient: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md)
    ) {
        Text(
            text = "Any ingredients you dislike?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "(Select all that apply)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search ingredients...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(spacing.sm),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onAddCustomIngredient(searchQuery) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(spacing.md))

        // Common dislikes section
        Text(
            text = "Common dislikes:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            CommonDislikedIngredients.ingredients.forEach { (name, englishName) ->
                val isSelected = name in dislikedIngredients
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleIngredient(name) },
                    label = {
                        Column {
                            Text(name)
                            Text(
                                text = "($englishName)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }

        // Show selected custom ingredients that aren't in common list
        val customIngredients = dislikedIngredients.filter { ingredient ->
            CommonDislikedIngredients.ingredients.none { it.first == ingredient }
        }
        if (customIngredients.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.lg))
            Text(
                text = "Custom:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                customIngredients.forEach { ingredient ->
                    FilterChip(
                        selected = true,
                        onClick = { onToggleIngredient(ingredient) },
                        label = { Text(ingredient) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        }

        if (dislikedIngredients.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.lg))
            Text(
                text = "Selected: ${dislikedIngredients.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// endregion

// region Step 5: Cooking Time

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CookingTimeStep(
    weekdayCookingTime: Int,
    weekendCookingTime: Int,
    busyDays: Set<DayOfWeek>,
    onWeekdayTimeChange: (Int) -> Unit,
    onWeekendTimeChange: (Int) -> Unit,
    onToggleBusyDay: (DayOfWeek) -> Unit
) {
    val cookingTimeOptions = listOf(15, 30, 45, 60, 90)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md)
    ) {
        Text(
            text = "How much time do you have\nfor cooking?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Weekdays dropdown
        Text(
            text = "Weekdays:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        var weekdayExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = weekdayExpanded,
            onExpandedChange = { weekdayExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "$weekdayCookingTime minutes",
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekdayExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(spacing.sm)
            )
            ExposedDropdownMenu(
                expanded = weekdayExpanded,
                onDismissRequest = { weekdayExpanded = false }
            ) {
                cookingTimeOptions.forEach { time ->
                    DropdownMenuItem(
                        text = { Text("$time minutes") },
                        onClick = {
                            onWeekdayTimeChange(time)
                            weekdayExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        // Weekends dropdown
        Text(
            text = "Weekends:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        var weekendExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = weekendExpanded,
            onExpandedChange = { weekendExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "$weekendCookingTime minutes",
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekendExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(spacing.sm)
            )
            ExposedDropdownMenu(
                expanded = weekendExpanded,
                onDismissRequest = { weekendExpanded = false }
            ) {
                cookingTimeOptions.forEach { time ->
                    DropdownMenuItem(
                        text = { Text("$time minutes") },
                        onClick = {
                            onWeekendTimeChange(time)
                            weekendExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Busy days selection
        Text(
            text = "Busy days (quick meals only):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in busyDays
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleBusyDay(day) },
                    label = { Text(day.shortName) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

// endregion

// region Generating Screen

@Composable
private fun GeneratingScreen(progress: GeneratingProgress) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(spacing.xl)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AppLogo(modifier = Modifier.size(80.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            Text(
                text = "Creating your perfect\nmeal plan...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.xl))

            // Progress items
            GeneratingProgressItem(
                text = "Analyzing preferences",
                isActive = progress.analyzingPreferences,
                isDone = progress.analyzingPreferencesDone
            )
            GeneratingProgressItem(
                text = "Checking festivals",
                isActive = progress.checkingFestivals,
                isDone = progress.checkingFestivalsDone
            )
            GeneratingProgressItem(
                text = "Generating recipes",
                isActive = progress.generatingRecipes,
                isDone = progress.generatingRecipesDone
            )
            GeneratingProgressItem(
                text = "Building grocery list",
                isActive = progress.buildingGroceryList,
                isDone = progress.buildingGroceryListDone
            )
        }
    }
}

@Composable
private fun GeneratingProgressItem(
    text: String,
    isActive: Boolean,
    isDone: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDone -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                isActive -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                else -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(spacing.md))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                isDone -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.onBackground
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// endregion

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun GeneratingScreenPreview() {
    RasoiAITheme {
        GeneratingScreen(
            progress = GeneratingProgress(
                analyzingPreferencesDone = true,
                checkingFestivalsDone = true,
                generatingRecipes = true
            )
        )
    }
}

// endregion
