package dev.biserman.planet.language

class Word(var phonetics: List<Segment>, var etymology: List<Word>)
class Name(var components: List<Word>)
