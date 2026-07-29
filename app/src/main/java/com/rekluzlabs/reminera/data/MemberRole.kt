package com.rekluzlabs.reminera.data

data class MemberRole(
    val key: String,
    val displayName: String
)

fun defaultRolesForGroupType(groupType: String): List<MemberRole> {
    return when (groupType) {
        "GREAT_GRANDPARENT" -> listOf(
            MemberRole("GREAT_GRANDFATHER", "Great Grandfather"),
            MemberRole("GREAT_GRANDMOTHER", "Great Grandmother")
        )
        "GRANDPARENTS" -> listOf(
            MemberRole("MATERNAL_GRANDFATHER", "Maternal Grandfather"),
            MemberRole("MATERNAL_GRANDMOTHER", "Maternal Grandmother"),
            MemberRole("PATERNAL_GRANDFATHER", "Paternal Grandfather"),
            MemberRole("PATERNAL_GRANDMOTHER", "Paternal Grandmother")
        )
        "PARENTS" -> listOf(
            MemberRole("FATHER", "Father"),
            MemberRole("MOTHER", "Mother"),
            MemberRole("CUSTOM", "Custom")
        )
        "SIBLINGS" -> listOf(
            MemberRole("BROTHER", "Brother"),
            MemberRole("SISTER", "Sister"),
            MemberRole("CUSTOM", "Custom")
        )
        "CHILDREN" -> listOf(
            MemberRole("CHILD_1", "Child 1"),
            MemberRole("CHILD_2", "Child 2"),
            MemberRole("CHILD_3", "Child 3"),
            MemberRole("CHILD_4", "Child 4"),
            MemberRole("CHILD_5", "Child 5"),
            MemberRole("CUSTOM", "Custom")
        )
        "FRIENDS" -> emptyList()
        "RELATIVES" -> listOf(
            MemberRole("UNCLE", "Uncle"),
            MemberRole("AUNT", "Aunt"),
            MemberRole("COUSIN", "Cousin"),
            MemberRole("NEPHEW", "Nephew"),
            MemberRole("NIECE", "Niece"),
            MemberRole("CUSTOM", "Custom")
        )
        "CUSTOM" -> emptyList()
        else -> emptyList()
    }
}
