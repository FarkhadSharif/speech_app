package com.farkhad.speechapp.audio

enum class WordStatus {
    CORRECT,        // Matches target word
    MISPRONOUNCED,  // A word was spoken but doesn't match
    OMITTED         // Word was not spoken
}

enum class CharStatus {
    CORRECT,
    WRONG
}

data class CharResult(
    val char: Char,
    val status: CharStatus
)

data class WordResult(
    val word: String,
    val status: WordStatus,
    val charResults: List<CharResult>,
    val spokenWord: String? = null
)

object PronunciationEvaluator {
    
    fun evaluate(target: String, recognized: String): List<WordResult> {
        val targetWords = normalizeToWords(target)
        val recognizedWords = normalizeToWords(recognized)
        
        val results = mutableListOf<WordResult>()
        
        var recognizedIdx = 0
        for (targetWord in targetWords) {
            if (recognizedIdx < recognizedWords.size) {
                val spoken = recognizedWords[recognizedIdx]
                val charAnalysis = evaluateChars(targetWord, spoken)
                val isWordCorrect = charAnalysis.all { it.status == CharStatus.CORRECT } && targetWord.length == spoken.length
                
                results.add(
                    WordResult(
                        word = targetWord,
                        status = if (isWordCorrect) WordStatus.CORRECT else WordStatus.MISPRONOUNCED,
                        charResults = charAnalysis,
                        spokenWord = spoken
                    )
                )
                recognizedIdx++
            } else {
                // Word omitted
                results.add(
                    WordResult(
                        word = targetWord,
                        status = WordStatus.OMITTED,
                        charResults = targetWord.map { CharResult(it, CharStatus.WRONG) }
                    )
                )
            }
        }
        
        return results
    }
    
    fun isOverallSuccess(results: List<WordResult>): Boolean {
        if (results.isEmpty()) return false
        val correctCount = results.count { it.status == WordStatus.CORRECT }
        return (correctCount.toFloat() / results.size) >= 0.8f
    }
    
    private fun evaluateChars(target: String, spoken: String): List<CharResult> {
        val results = mutableListOf<CharResult>()
        val maxLen = target.length
        
        for (i in 0 until maxLen) {
            val tChar = target[i]
            val sChar = if (i < spoken.length) spoken[i] else null
            
            if (sChar != null && isCharMatch(tChar, sChar)) {
                results.add(CharResult(tChar, CharStatus.CORRECT))
            } else {
                results.add(CharResult(tChar, CharStatus.WRONG))
            }
        }
        return results
    }
    
    private fun isCharMatch(c1: Char, c2: Char): Boolean {
        if (c1.lowercaseChar() == c2.lowercaseChar()) return true
        
        // Kazakh specific similar sounds fuzzy match
        val pairs = listOf(
            setOf('қ', 'к'),
            setOf('ғ', 'г'),
            setOf('ү', 'ұ'),
            setOf('ө', 'о'),
            setOf('і', 'и'),
            setOf('ә', 'а'),
            setOf('ң', 'н'),
        )
        
        return pairs.any { it.contains(c1.lowercaseChar()) && it.contains(c2.lowercaseChar()) }
    }
    
    private fun normalizeToWords(text: String): List<String> = text
        .lowercase()
        .replace(Regex("[^а-яәіңғүұқөһa-z0-9 ]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
}
