package com.painitefb.app

import kotlin.random.Random

object UsaGenerator {
    private val male = listOf(
        "James", "Ethan", "Liam", "Noah", "Oliver", "William", "Benjamin", "Lucas", "Henry", "Alexander",
        "Mason", "Michael", "Daniel", "Jacob", "Logan", "Jackson", "Levi", "Sebastian", "Jack", "Samuel"
    )
    private val female = listOf(
        "Emma", "Olivia", "Ava", "Sophia", "Isabella", "Mia", "Charlotte", "Amelia", "Harper", "Evelyn",
        "Abigail", "Emily", "Ella", "Elizabeth", "Camila", "Luna", "Sofia", "Avery", "Scarlett", "Grace"
    )
    private val last = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin"
    )

    fun generate(femaleGender: Boolean = true): String {
        val first = if (femaleGender) female.random() else male.random()
        val lastName = last.random()
        return "Name: $first $lastName"
    }
}
