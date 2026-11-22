package com.example.util

data class QuoteEntry(val text: String, val author: String)

object QuotesBank {
    val quotes = listOf(
        QuoteEntry("Simplicity is about subtracting the obvious and adding the meaningful.", "John Maeda"),
        QuoteEntry("Focus is a matter of deciding what things you are not going to do.", "John Carmack"),
        QuoteEntry("Perfection is achieved not when there is nothing more to add, but nothing left to take away.", "Antoine de Saint-Exupery"),
        QuoteEntry("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs"),
        QuoteEntry("The secret of getting ahead is getting started.", "Mark Twain"),
        QuoteEntry("Quiet the mind, and the soul will speak.", "Ma Jaya Sati Bhagavati"),
        QuoteEntry("Do less, better.", "Dieter Rams"),
        QuoteEntry("Small daily improvements over time lead to stunning results.", "Robin Sharma")
    )

    fun get(index: Int): QuoteEntry = quotes[((index % quotes.size) + quotes.size) % quotes.size]
}
