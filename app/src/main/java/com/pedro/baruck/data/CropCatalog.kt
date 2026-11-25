package com.pedro.baruck.data

/** Uma cultura do catálogo. [emoji] é usado como ícone leve nas listas. */
data class CatalogCrop(
    val key: String,
    val name: String,
    val emoji: String,
    val category: String,
    val cycleDays: Int // ciclo médio até a colheita, para estimar a data
)

/** Catálogo amplo de culturas de hortifruti comuns no Brasil. */
object CropCatalog {
    val all: List<CatalogCrop> = listOf(
        // Folhosas
        CatalogCrop("alface", "Alface", "🥬", "Folhosas", 45),
        CatalogCrop("rucula", "Rúcula", "🌿", "Folhosas", 40),
        CatalogCrop("couve", "Couve", "🥬", "Folhosas", 60),
        CatalogCrop("espinafre", "Espinafre", "🌿", "Folhosas", 50),
        CatalogCrop("agriao", "Agrião", "🌿", "Folhosas", 55),
        CatalogCrop("acelga", "Acelga", "🥬", "Folhosas", 60),
        CatalogCrop("almeirao", "Almeirão", "🌿", "Folhosas", 55),
        CatalogCrop("chicoria", "Chicória", "🥬", "Folhosas", 55),
        CatalogCrop("repolho", "Repolho", "🥬", "Folhosas", 90),
        // Temperos / ervas
        CatalogCrop("cheiro_verde", "Cheiro-verde", "🌿", "Temperos", 50),
        CatalogCrop("salsa", "Salsa", "🌿", "Temperos", 70),
        CatalogCrop("cebolinha", "Cebolinha", "🧅", "Temperos", 60),
        CatalogCrop("coentro", "Coentro", "🌿", "Temperos", 45),
        CatalogCrop("manjericao", "Manjericão", "🌿", "Temperos", 60),
        CatalogCrop("hortela", "Hortelã", "🌿", "Temperos", 60),
        CatalogCrop("alecrim", "Alecrim", "🌿", "Temperos", 120),
        // Frutos
        CatalogCrop("tomate", "Tomate", "🍅", "Frutos", 110),
        CatalogCrop("tomate_cereja", "Tomate-cereja", "🍅", "Frutos", 95),
        CatalogCrop("pimentao", "Pimentão", "🫑", "Frutos", 120),
        CatalogCrop("pimenta", "Pimenta", "🌶️", "Frutos", 110),
        CatalogCrop("pepino", "Pepino", "🥒", "Frutos", 60),
        CatalogCrop("abobrinha", "Abobrinha", "🥒", "Frutos", 55),
        CatalogCrop("abobora", "Abóbora", "🎃", "Frutos", 120),
        CatalogCrop("berinjela", "Berinjela", "🍆", "Frutos", 110),
        CatalogCrop("quiabo", "Quiabo", "🌿", "Frutos", 70),
        CatalogCrop("chuchu", "Chuchu", "🥒", "Frutos", 90),
        CatalogCrop("milho_verde", "Milho-verde", "🌽", "Frutos", 100),
        CatalogCrop("vagem", "Vagem", "🫘", "Frutos", 70),
        CatalogCrop("ervilha", "Ervilha", "🫘", "Frutos", 75),
        // Raízes / tubérculos / bulbos
        CatalogCrop("cenoura", "Cenoura", "🥕", "Raízes", 90),
        CatalogCrop("beterraba", "Beterraba", "🫑", "Raízes", 80),
        CatalogCrop("rabanete", "Rabanete", "🌿", "Raízes", 30),
        CatalogCrop("batata", "Batata", "🥔", "Raízes", 100),
        CatalogCrop("batata_doce", "Batata-doce", "🍠", "Raízes", 120),
        CatalogCrop("mandioca", "Mandioca", "🥔", "Raízes", 300),
        CatalogCrop("inhame", "Inhame", "🥔", "Raízes", 240),
        CatalogCrop("cebola", "Cebola", "🧅", "Raízes", 120),
        CatalogCrop("alho", "Alho", "🧄", "Raízes", 150),
        CatalogCrop("gengibre", "Gengibre", "🧄", "Raízes", 240),
        // Brássicas
        CatalogCrop("brocolis", "Brócolis", "🥦", "Brássicas", 90),
        CatalogCrop("couve_flor", "Couve-flor", "🥦", "Brássicas", 90),
        // Frutas
        CatalogCrop("morango", "Morango", "🍓", "Frutas", 70),
        CatalogCrop("melancia", "Melancia", "🍉", "Frutas", 90),
        CatalogCrop("melao", "Melão", "🍈", "Frutas", 80),
        CatalogCrop("maracuja", "Maracujá", "🟣", "Frutas", 240),
        CatalogCrop("mamao", "Mamão", "🫐", "Frutas", 300),
        CatalogCrop("banana", "Banana", "🍌", "Frutas", 300),
        CatalogCrop("abacaxi", "Abacaxi", "🍍", "Frutas", 400),
        CatalogCrop("uva", "Uva", "🍇", "Frutas", 150),
        CatalogCrop("limao", "Limão", "🍋", "Frutas", 240),
        CatalogCrop("laranja", "Laranja", "🍊", "Frutas", 300),
        CatalogCrop("goiaba", "Goiaba", "🍈", "Frutas", 180),
        CatalogCrop("figo", "Figo", "🏷️", "Frutas", 150)
    )

    val categories: List<String> = all.map { it.category }.distinct()

    fun byKey(key: String): CatalogCrop? = all.firstOrNull { it.key == key }

    fun search(q: String): List<CatalogCrop> {
        if (q.isBlank()) return all
        val t = q.trim().lowercase()
        return all.filter { it.name.lowercase().contains(t) || it.category.lowercase().contains(t) }
    }
}
