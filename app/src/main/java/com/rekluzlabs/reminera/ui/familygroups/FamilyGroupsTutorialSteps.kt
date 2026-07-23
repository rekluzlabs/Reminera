package com.rekluzlabs.reminera.ui.familygroups

import com.rekluzlabs.reminera.ui.tutorial.TutorialStep

val familyGroupsTutorialSteps = listOf(
    TutorialStep(
        id = "fg_overview",
        targetKey = null,
        title = "Family Groups",
        description = "Organize recordings by branch of the family — grandparents, cousins, in-laws, however you want to split it. Recordings and photos you add later can be tagged to a group."
    ),
    TutorialStep(
        id = "fg_add_button",
        targetKey = "add_family_group_button",
        title = "Start your first group",
        description = "Tap here to create a group. Give it a name and add the family members who belong in it.",
        dismissOnRealAction = true
    ),
    TutorialStep(
        id = "fg_name_field",
        targetKey = "group_name_field",
        title = "Name the group",
        description = "e.g. \"Mom's Side\" or \"The Tanaka Family\" — anything that helps you recognize it later."
    ),
    TutorialStep(
        id = "fg_add_members",
        targetKey = "add_members_button",
        title = "Add family members",
        description = "Add people now, or skip and add them anytime from inside the group."
    ),
    TutorialStep(
        id = "fg_save",
        targetKey = "save_group_button",
        title = "Save your group",
        description = "Once saved, you can start attaching recordings, photos, and memories to this group."
    )
)
