package com.farkhad.speechapp.model

data class CurriculumLevel(
    val id: Int,
    val title: String,
    val subtitle: String,
    val focus: String,
    val emoji: String,
    val activities: List<GameActivity>,
    val imageUrl: String,
    val vocabulary: List<VocabularyWord> = emptyList(),
)

data class VocabularyWord(
    val id: String,
    val word: String,
    val emoji: String,
    val definition: String,
    val example: String,
    val imageUrl: String = "",
)

data class GameActivity(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val minutes: Int,
    val rounds: List<ExerciseRound>,
    val imageUrl: String = "",
)

sealed class ExerciseRound {
    abstract val id: String
    abstract val instruction: String
}

data class ChoiceRound(
    override val id: String,
    override val instruction: String,
    val speech: String,
    val display: String,
    val options: List<String>,
    val correctIndex: Int,
    val hint: String,
) : ExerciseRound()

data class TapCountRound(
    override val id: String,
    override val instruction: String,
    val word: String,
    val emoji: String,
    val correctTaps: Int,
    val hint: String,
) : ExerciseRound()

data class ArrangeWordsRound(
    override val id: String,
    override val instruction: String,
    val speech: String,
    val wordsInOrder: List<String>,
    val hint: String,
) : ExerciseRound()

data class StoryCard(
    val emoji: String,
    val text: String,
)

data class StoryOrderRound(
    override val id: String,
    override val instruction: String,
    val speech: String,
    val cardsInOrder: List<StoryCard>,
    val hint: String,
) : ExerciseRound()

data class VoiceRound(
    override val id: String,
    override val instruction: String,
    val modelText: String,
    val picture: String,
    val childEncouragement: String,
) : ExerciseRound()

object Curriculum {
    val levels: List<CurriculumLevel> = listOf(
        CurriculumLevel(
            id = 1,
            title = "Кең дала",
            subtitle = "Сөзді естіп, суретін табамыз",
            focus = "Тыңдау • алғашқы сөздер • 1–2 буын",
            emoji = "🌾",
            imageUrl = "https://images.unsplash.com/photo-1542332213-9b5a5a3fad35?auto=format&fit=crop&q=80&w=1000",
            vocabulary = listOf(
                VocabularyWord("v1", "Алма", "🍎", "Тәтті, қызыл немесе жасыл жеміс.", "Мен қызыл алма жеймін."),
                VocabularyWord("v2", "Бала", "🧒", "Кішкентай адам.", "Бала доп ойнайды."),
                VocabularyWord("v3", "Су", "💧", "Мөлдір, ішуге пайдалы сұйықтық.", "Су ішу денсаулыққа жақсы."),
                VocabularyWord("v4", "Доп", "⚽", "Ойынға арналған домалақ зат.", "Менің добым биікке секіреді."),
                VocabularyWord("v1a", "Нан", "🍞", "Астықтан жасалған негізгі тағам.", "Нан — ас атасы."),
                VocabularyWord("v1b", "Сүт", "🥛", "Ақ түсті пайдалы сусын.", "Сүт ішу балаға өте пайдалы."),
                VocabularyWord("v1c", "Ойын", "🎮", "Көңіл көтеруге арналған әрекет.", "Біз бірге қызықты ойын ойнаймыз."),
                VocabularyWord("v1d", "Аю", "🐻", "Үлкен, қорбаңдаған жануар.", "Аю балды жақсы көреді."),
                VocabularyWord("v1e", "Күн", "☀️", "Жарық пен жылу беретін жұлдыз.", "Күн шығып, айнала жап-жарық болды.")
            ),
            activities = listOf(
                activity(
                    id = "l1_listen",
                    title = "Жемісті ұста",
                    subtitle = "Сөзді тыңдап, дұрыс жемісті таңда",
                    emoji = "🍎",
                    minutes = 3,
                    choice("l1a1", "Дұрыс суретті таңда", "Алманы тап.", "Қайсысы алма?", "🍎 Алма", "🍎 Алма", "🍌 Банан", hint = "Алма — қызыл немесе жасыл домалақ жеміс."),
                    choice("l1a2", "Дұрыс суретті таңда", "Бананды тап.", "Қайсысы банан?", "🍌 Банан", "🍎 Алма", "🍌 Банан", hint = "Банан — ұзын сары жеміс."),
                    choice("l1a3", "Дұрыс суретті таңда", "Қарбызды тап.", "Қайсысы қарбыз?", "🍉 Қарбыз", "🍐 Алмұрт", "🍉 Қарбыз", "🍇 Жүзім", hint = "Қарбыздың сырты жасыл, іші қызыл."),
                    choice("l1a4", "Дұрыс суретті таңда", "Алмұртты тап.", "Қайсысы алмұрт?", "🍐 Алмұрт", "🍊 Апельсин", "🍐 Алмұрт", "🍓 Құлпынай", hint = "Алмұрттың төменгі жағы жалпақ болады."),
                ),
                activity(
                    id = "l1_voice",
                    title = "Мен де айтамын",
                    subtitle = "Тыңда, сосын асықпай қайтала",
                    emoji = "🗣️",
                    minutes = 3,
                    voice("l1b1", "Сөзді тыңдап, қайтала", "алма", "🍎", "Жарайсың! Керемет айттың! 🍎"),
                    voice("l1b2", "Сөзді тыңдап, қайтала", "бала", "🧒", "Сен нағыз батырсың! 🐎"),
                    voice("l1b3", "Сөзді тыңдап, қайтала", "доп", "⚽", "Доптай домалап, биікке шырқа! ⚽"),
                    voice("l1b4", "Сөзді тыңдап, қайтала", "су", "💧", "Судай мөлдір, таза бол! 💧"),
                ),
                activity(
                    id = "l1_syllables",
                    title = "Буын барабаны",
                    subtitle = "Сөздің бөліктерін шапалақпен сана",
                    emoji = "🥁",
                    minutes = 3,
                    taps("l1c1", "Әр буынға бір рет бас", "доп", "⚽", 1, "Доп — бір қысқа буын."),
                    taps("l1c2", "Әр буынға бір рет бас", "ал-ма", "🍎", 2, "Ал-ма — екі буын."),
                    taps("l1c3", "Әр буынға бір рет бас", "ба-ла", "🧒", 2, "Ба-ла — екі буын."),
                    taps("l1c4", "Әр буынға бір рет бас", "қар-быз", "🍉", 2, "Қар-быз — екі буын."),
                ),
            ),
        ),
        CurriculumLevel(
            id = 2,
            title = "Ауылдағы кеш",
            subtitle = "Мағына, әрекет және сезімді түсінеміз",
            focus = "Сөздік қор • санаттар • бір қадамдық нұсқау",
            emoji = "⛺",
            imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&q=80&w=1000",
            vocabulary = listOf(
                VocabularyWord("v5", "Мысық", "🐱", "Үйде тұратын кішкентай жануар.", "Мысық сүт ішеді."),
                VocabularyWord("v6", "Жеміс", "🍎", "Ағашта өсетін дәмді тағам.", "Жемістерде дәрумен көп."),
                VocabularyWord("v7", "Киім", "🧥", "Үстімізге киетін заттар.", "Жылы киім суықтан қорғайды."),
                VocabularyWord("v8", "Себет", "🧺", "Заттарды салуға арналған ыдыс.", "Себетке алма салдым."),
                VocabularyWord("v2a", "Ит", "🐶", "Адамның ең жақын досы.", "Менің итім өте ақылды."),
                VocabularyWord("v2b", "Үй", "🏠", "Біз тұратын мекен.", "Біздің үйіміз өте үлкен."),
                VocabularyWord("v2c", "Ағаш", "🌳", "Табиғаттың бір бөлігі.", "Аулада үлкен ағаш өсіп тұр."),
                VocabularyWord("v2d", "Гүл", "🌸", "Әдемі өсімдік.", "Бақшада хош іісті гүлдер өсіп тұр."),
                VocabularyWord("v2e", "Көлік", "🚗", "Адамдарды таситын көлік құралы.", "Әкемнің көлігі өте жылдам."),
                VocabularyWord("v2f", "Құс", "🐦", "Аспанда ұшатын жануар.", "Құстар таңертең ән салады.")
            ),
            activities = listOf(
                activity(
                    id = "l2_categories",
                    title = "Сиқырлы себет",
                    subtitle = "Бір топқа жататын затты тап",
                    emoji = "🧺",
                    minutes = 4,
                    choice("l2a1", "Сұрақты тыңда", "Қайсысы жеміс?", "Жемісті тап", "🍎 Алма", "🍎 Алма", "🚗 Көлік", "👟 Аяқ киім", hint = "Жемісті жеуге болады."),
                    choice("l2a2", "Сұрақты тыңда", "Қайсысы жануар?", "Жануарды тап", "🐱 Мысық", "🧢 Бас киім", "🐱 Мысық", "🥄 Қасық", hint = "Мысық — үй жануары."),
                    choice("l2a3", "Сұрақты тыңда", "Қайсысы киім?", "Киімді тап", "🧥 Күртеше", "🍌 Банан", "🚌 Автобус", "🧥 Күртеше", hint = "Күртешені үстімізге киеміз."),
                    choice("l2a4", "Артық затты тап", "Қайсысы бұл топқа жатпайды? Алма, алмұрт, доп.", "Артық зат қайсы?", "⚽ Доп", "🍎 Алма", "🍐 Алмұрт", "⚽ Доп", hint = "Алма мен алмұрт — жеміс, доп — ойыншық."),
                ),
                activity(
                    id = "l2_directions",
                    title = "Жолдан өт",
                    subtitle = "Қысқа нұсқауды тыңдап орында",
                    emoji = "🚦",
                    minutes = 4,
                    choice("l2b1", "Нұсқауды орында", "Көк допты таңда.", "Қай доп?", "🔵 Көк доп", "🔴 Қызыл доп", "🔵 Көк доп", "🟡 Сары доп", hint = "Көк түсті есіңе түсір."),
                    choice("l2b2", "Нұсқауды орында", "Үлкен жануарды таңда.", "Қайсысы үлкен?", "🐘 Піл", "🐭 Тышқан", "🐘 Піл", "🐞 Қоңыз", hint = "Піл тышқаннан үлкен."),
                    choice("l2b3", "Нұсқауды орында", "Үстелде тұрған затты таңда.", "Үстелде не тұр?", "📘 Кітап", "🐟 Балық", "📘 Кітап", "☁️ Бұлт", hint = "Кітап үстелдің үстінде жатыр."),
                    choice("l2b4", "Нұсқауды орында", "Қызыл алманы таңда.", "Түс пен затты бірге тыңда", "🍎 Қызыл алма", "🍏 Жасыл алма", "🍌 Сары банан", "🍎 Қызыл алма", hint = "Екі белгіні тыңда: қызыл және алма."),
                ),
                activity(
                    id = "l2_emotions",
                    title = "Эмоджи ойыны",
                    subtitle = "Оқиғадағы сезімді анықта",
                    emoji = "😊",
                    minutes = 4,
                    choice("l2c1", "Оқиғаны тыңда", "Балаға сыйлық берді. Ол қалай сезінеді?", "Бала сыйлық алды 🎁", "😊 Қуанышты", "😊 Қуанышты", "😢 Мұңды", "😠 Ашулы", hint = "Сыйлық алғанда бала қуанады."),
                    choice("l2c2", "Оқиғаны тыңда", "Ойыншық сынып қалды. Бала қалай сезінеді?", "Ойыншық сынып қалды 🧸", "😢 Мұңды", "😴 Ұйқылы", "😢 Мұңды", "😊 Қуанышты", hint = "Жақсы көретін зат сынғанда көңіл түсуі мүмкін."),
                    choice("l2c3", "Оқиғаны тыңда", "Қатты дыбыс шықты. Бала қалай сезінеді?", "Кенет қатты дыбыс шықты 💥", "😨 Қорықты", "😨 Қорықты", "😊 Қуанды", "😋 Дәмді", hint = "Кенет дыбыс қорқытуы мүмкін."),
                    choice("l2c4", "Оқиғаны тыңда", "Досы ойыншығын sұрамай алды. Бала қалай сезінуі мүмкін?", "Досы sұрамай алды", "😠 Ренжіді", "😠 Ренжіді", "😴 Ұйықтады", "🤣 Күлді", hint = "Сезімді атау оны тыныш жеткізуге көмектеседі."),
                ),
            ),
        ),
        CurriculumLevel(
            id = 3,
            title = "Алатау шыңдары",
            subtitle = "Сөздің басын, буынын және ұйқасын естиміз",
            focus = "Фонологиялық сана • алғашқы дыбыс • ұйқас",
            emoji = "🏔️",
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&q=80&w=1000",
            vocabulary = listOf(
                VocabularyWord("v9", "Балық", "🐟", "Суда жүзетін жануар.", "Балық суда жүзеді."),
                VocabularyWord("v10", "Аю", "🐻", "Орманда тұратын үлкен аң.", "Аю балды жақсы көреді."),
                VocabularyWord("v11", "Үй", "🏠", "Адамдар тұратын жер.", "Біздің үйіміз кең әрі жарық."),
                VocabularyWord("v12", "Терезе", "🪟", "Жарық түсетін үйдің бөлігі.", "Терезеден дала көрінеді."),
                VocabularyWord("v3a", "Тау", "🏔️", "Өте биік жер бедері.", "Алатаудың шыңдары өте биік."),
                VocabularyWord("v3b", "Бұлт", "☁️", "Аспандағы бу тобы.", "Аспанда ақ бұлттар көшкен."),
                VocabularyWord("v3c", "Қар", "❄️", "Қыста жауатын ақ жауын.", "Қыста ақ ұлпа қар жауады."),
                VocabularyWord("v3d", "Шаңғы", "⛷️", "Қарда сырғанауға арналған құрал.", "Қыста тауда шаңғы тебеміз."),
                VocabularyWord("v3e", "Шырша", "🌲", "Қыста да жасыл болып тұратын ағаш.", "Орманда үлкен шыршалар өседі.")
            ),
            activities = listOf(
                activity(
                    id = "l3_first_sound",
                    title = "Алғашқы дыбыс",
                    subtitle = "Сөз қандай дыбыстан басталады?",
                    emoji = "🔤",
                    minutes = 4,
                    choice("l3a1", "Алғашқы дыбысты тап", "Алма. Алма қандай дыбыстан басталады?", "🍎 Алма", "А", "А", "Б", "М", hint = "А-а-алма деп sозып айт."),
                    choice("l3a2", "Алғашқы дыбысты тап", "Балық. Балық қандай дыбыстан басталады?", "🐟 Балық", "Б", "Д", "Б", "Қ", hint = "Б-б-балық."),
                    choice("l3a3", "Алғашқы дыбысты тап", "Доп. Доп қандай дыбыстан басталады?", "⚽ Доп", "Д", "Т", "М", "Д", hint = "Д-д-доп."),
                    choice("l3a4", "Алғашқы дыбысты тап", "Мысық. Мысық қандай дыбыстан басталады?", "🐱 Мысық", "М", "Н", "М", "С", hint = "М-м-мысық."),
                    choice("l3a5", "Бірдей дыбыстан басталатын сөзді тап", "Алма sөзі sияқты а дыбыsынан басталатын sөзді тап.", "Алма → ?", "🐻 Аю", "🐻 Аю", "🐟 Балық", "🏠 Үй", hint = "Алма және аю — екеуі де а дыбыsынан басталады."),
                ),
                activity(
                    id = "l3_rhyme",
                    title = "Ұйқасын тап",
                    subtitle = "Соңы ұқсас естілетін сөздерді тап",
                    emoji = "🎶",
                    minutes = 4,
                    choice("l3b1", "Ұйқас сөзді тап", "Бала сөзіне ұйқас сөзді тап.", "Бала — ?", "Қала", "Қала", "Кітап", "Терезе", hint = "Ба-ла, қа-ла — sоңғы бөлігі ұқsайды."),
                    choice("l3b2", "Ұйқас сөзді тап", "Тас сөзіне ұйқас сөзді тап.", "Тас — ?", "Бас", "Гүл", "Бас", "Доп", hint = "Тас, бас — екеуі де аs деп аяқталады."),
                    choice("l3b3", "Ұйқас сөзді тап", "Гүл сөзіне ұйқас сөзді тап.", "Гүл — ?", "Күл", "Күн", "Күл", "Құс", hint = "Гүл, күл — үл дыбыsтарымен аяқталады."),
                    choice("l3b4", "Ұйқас сөзді тап", "Қой сөзіне ұйқас сөзді тап.", "Қой — ?", "Той", "Той", "Үй", "Ай", hint = "Қой, той — ой деп аяқталады."),
                ),
                activity(
                    id = "l3_rhythm",
                    title = "Буын пойызы",
                    subtitle = "Ұзындау сөздерді буынға бөл",
                    emoji = "🚂",
                    minutes = 4,
                    taps("l3c1", "Әр буынға бір рет бас", "құл-пы-най", "🍓", 3, "Құл-пы-най — үш буын."),
                    taps("l3c2", "Әр android буынға бір рет бас", "те-ре-зе", "🪟", 3, "Те-ре-зе — үш буын."),
                    taps("l3c3", "Әр буынға бір рет бас", "ма-ши-на", "🚗", 3, "Ма-ши-на — үш буын."),
                    taps("l3c4", "Әр буынға бір рет бас", "кө-бе-лек", "🦋", 3, "Кө-бе-лек — үш буын."),
                    taps("l3c5", "Әр буынға бір рет бас", "ба-ла-бақ-ша", "🏫", 4, "Ба-ла-бақ-ша — төрт буын."),
                ),
            ),
        ),
        CurriculumLevel(
            id = 4,
            title = "Көлсай көлдері",
            subtitle = "Сөздерді байланыстырып, толық ой айтамыз",
            focus = "3–5 сөздік сөйлем • кім? не істеді? қайда?",
            emoji = "💧",
            imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&q=80&w=1000",
            vocabulary = listOf(
                VocabularyWord("v13", "Көл", "💧", "Табиғи су қоймасы.", "Көлсай көлі өте әдемі."),
                VocabularyWord("v14", "Қыз", "👧", "Кішкентай адам (әйел жынысты).", "Қыз әдемі көйлек киді."),
                VocabularyWord("v15", "Әже", "👵", "Әкенің немесе ананың анасы.", "Әжем маған ертегі айтады."),
                VocabularyWord("v16", "Бауырсақ", "🥟", "Қазақтың ұлттық тағамы.", "Ыстық бауырсақ өте дәмді."),
                VocabularyWord("v4a", "Гүл", "🌸", "Әдемі өсімдік.", "Бақшада әдемі гүлдер өсіп тұр."),
                VocabularyWord("v4b", "Күн", "☀️", "Жарық беретін жұлдыз.", "Күн шығып, айнала жап-жарық болды."),
                VocabularyWord("v4c", "Орындық", "🪑", "Отыруға арналған жиһаз.", "Орындықта кішкентай күшік отыр."),
                VocabularyWord("v4d", "Дәптер", "📒", "Жазуға арналған қағаздар жинағы.", "Дәптерге әдемілеп жаздым."),
                VocabularyWord("v4e", "Қалам", "🖊️", "Жазу құралы.", "Көк қаламмен тапсырма орындадым.")
            ),
            activities = listOf(
                activity(
                    id = "l4_sentences",
                    title = "Сөйлем құрастыр",
                    subtitle = "Сөздерді дұрыс ретпен орналастыр",
                    emoji = "🧩",
                    minutes = 5,
                    arrange("l4a1", "Сөйлемді құрастыр", "Бала алма жеді.", "Бала", "алма", "жеді."),
                    arrange("l4a2", "Сөйлемді құрастыр", "Мысық sүт ішті.", "Мысық", "sүт", "ішті."),
                    arrange("l4a3", "Сөйлемді құрастыр", "Қыз қызыл допты алды.", "Қыз", "қызыл", "допты", "алды."),
                    arrange("l4a4", "Сөйлемді құрастыр", "Әдемі көбелек гүлге қонды.", "Әдемі", "көбелек", "гүлге", "қонды."),
                ),
                activity(
                    id = "l4_questions",
                    title = "Кім? Не істеді?",
                    subtitle = "Сөйлемді тыңдап, сұраққа жауап бер",
                    emoji = "❓",
                    minutes = 5,
                    choice("l4b1", "Сөйлемді тыңда", "Аsан аулада доп ойнады. Кім доп ойнады?", "Кім?", "Аsан", "Аsан", "Доп", "Аула", hint = "Кім? деген sұрақ адамды sұрайды."),
                    choice("l4b2", "Сөйлемді тыңда", "Әлия кітап оқыды. Әлия не істеді?", "Не істеді?", "Кітап оқыды", "Ұйықтады", "Кітап оқыды", "Жүгірді", hint = "Әрекетті білдіретін sөздерді ізде."),
                    choice("l4b3", "Сөйлемді тыңда", "Күшік орындықтың аsтында отыр. Күшік қайда?", "Қайда?", "Орындықтың аsтында", "Үsтелдің үsтінде", "Орындықтың аsтында", "Аулада", hint = "Аsтында — заттың төменгі жағында."),
                    choice("l4b4", "Сөйлемді тыңда", "Әже дәмді бауырsақ піsірді. Не піsірді?", "Не?", "Бауырsақ", "Әже", "Бауырsақ", "Ас үй", hint = "Не? деген sұрақ затты sұрайды."),
                    choice("l4b5", "Сөйлемді тыңда", "Балалар жаңбырдан кейін етік киді. Неліктен етік киді?", "Неліктен?", "Жер sу болды", "Күн ыsтық болды", "Жер sу болды", "Ұйқыsы келді", hint = "Жаңбырдан кейін жерде sу болады."),
                ),
                activity(
                    id = "l4_voice",
                    title = "Толық айтып көр",
                    subtitle = "Бір сөзді толық сөйлемге айналдыр",
                    emoji = "💬",
                    minutes = 4,
                    voice("l4c1", "Үлгіні тыңдап, қайтала", "Мен алма жеймін.", "🍎", "Сөздерің қандай әдемі! 🍎"),
                    voice("l4c2", "Үлгіні тыңдап, қайтала", "Көк доп домалап барады.", "🔵", "Керемет сөйлем құрадың! 🔵"),
                    voice("l4c3", "Үлгіні тыңдап, қайтала", "Мысық орындықтың астында отыр.", "🐱", "Сен өте зейіндісің! 🐱"),
                    voice("l4c4", "Үлгіні тыңдап, қайтала", "Мен досыма ойыншық бердім.", "🤝", "Достығың берік болсын! 🤝"),
                ),
            ),
        ),
        CurriculumLevel(
            id = 5,
            title = "Бурабай орманы",
            subtitle = "Оқиғаның ретін түсініп, сұраққа жауап береміз",
            focus = "Оқиға реті • себеп-салдар • сипаттау",
            emoji = "🌲",
            imageUrl = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&q=80&w=1000",
            vocabulary = listOf(
                VocabularyWord("v17", "Орман", "🌲", "Ағаштар көп өсетін жер.", "Орман ауасы өте таза."),
                VocabularyWord("v18", "Көбелек", "🦋", "Әдемі қанатты жәндік.", "Көбелек гүлге қонды."),
                VocabularyWord("v19", "Күртеше", "🧥", "Сырт киім.", "Мен жаңа күртеше кидім."),
                VocabularyWord("v20", "Қағаз", "📄", "Жазуға арналған парақ.", "Қағазға sурет sалдым."),
                VocabularyWord("v5a", "Құс", "🐦", "Ұша алатын жануар.", "Құстар аспанда еркін ұшады."),
                VocabularyWord("v5b", "Ат", "🐎", "Мінуге арналған үй жануары.", "Менің атамның үлкен аты бар."),
                VocabularyWord("v5c", "Дос", "👦👧", "Бірге ойнайтын жақын адам.", "Менің ең жақын досым бар."),
                VocabularyWord("v5d", "Бақша", "🌳", "Жеміс-жидек өсетін жер.", "Бақшада алмалар пісіп тұр."),
                VocabularyWord("v5e", "Ойыншық", "🧸", "Ойнауға арналған зат.", "Менің ең жақсы көретін ойыншығым — қонжық.")
            ),
            activities = listOf(
                activity(
                    id = "l5_story",
                    title = "Оқиғаны ретте",
                    subtitle = "Басы, ортасы және соңын тап",
                    emoji = "📚",
                    minutes = 5,
                    story("l5a1", "Таңғы істің ретін тап", "Алдымен оянды, кейін тіsін тазалады, sоңында таңғы аs ішті.", StoryCard("🛏️", "Оянды"), StoryCard("🪥", "Тісін тазалады"), StoryCard("🥣", "Таңғы ас ішті")),
                    story("l5a2", "Гүлдің өсу ретін тап", "Алдымен тұқым екті, кейін sу құйды, sоңында гүл өsті.", StoryCard("🌰", "Тұқым екті"), StoryCard("💧", "Су құйды"), StoryCard("🌷", "Гүл өсті")),
                    story("l5a3", "Қар ойынының ретін тап", "Алдымен күртеше киді, кейін далаға шықты, sоңында аққала жаsады.", StoryCard("🧥", "Күртеше киді"), StoryCard("🚪", "Далаға шықты"), StoryCard("⛄", "Аққала жасады")),
                    story("l5a4", "Сурет салу ретін тап", "Алдымен қағаз алды, кейін sурет sалды, sоңында sуретті анаsына көрsетті.", StoryCard("📄", "Қағаз алды"), StoryCard("🖍️", "Сурет салды"), StoryCard("🖼️", "Көрсетті")),
                ),
                activity(
                    id = "l5_comprehension",
                    title = "Ойлан да жауап бер",
                    subtitle = "Қысқа оқиғаны түсініп жауап бер",
                    emoji = "🤔",
                    minutes = 5,
                    choice("l5b1", "Оқиғаны тыңда", "Айша далаға шықты. Аsпанда қара бұлт болды. Ол қолшатыр алды. Неліктен?", "Айша неге қолшатыр алды?", "Жаңбыр жаууы мүмкін", "Күн ыстық", "Жаңбыр жаууы мүмкін", "Қар жауды", hint = "Қара бұлт жаңбырдың белгіsі болуы мүмкін."),
                    choice("l5b2", "Оқиғаны тыңда", "Марат күшіктің ыдыsына sу құйды. Күшік не іsтейді?", "Келеsі не болуы мүмкін?", "Су ішеді", "Кітап оқыды", "Су ішеді", "Ұшып кетеді", hint = "Ыдыsтағы sу жануар ішу үшін берілді."),
                    choice("l5b3", "Оқиғаны тыңда", "Дана мұзды күнге қойды. Біраздан кейін мұз sу болды. Не өзгерді?", "Не болды?", "Мұз еріді", "Су қатты", "Мұз еріді", "Күн сөнді", hint = "Жылы жерде мұз ериді."),
                    choice("l5b4", "Оқиғаны тыңда", "Екі бала бір доппен ойнағыsы келді. Олар не іsтей алады?", "Жақсы шешімді тап", "Кезекпен ойнайды", "Допты жасырады", "Ұрысады", "Кезекпен ойнайды", hint = "Кезекпен ойнау екеуіне де мүмкіндік береді."),
                    choice("l5b5", "Оқиғаны тыңда", "Әли терезеден құsтардың ұшып кеткенін көрді. Ағаштарда жапырақ азайды. Қай мезгіл?", "Қай мезгіл?", "Күз", "Жаз", "Күз", "Көктем", hint = "Күзде жапырақтар түsіп, кей құsтар жылы жаққа ұшады."),
                ),
                activity(
                    id = "l5_describe",
                    title = "Суретпен әңгіме",
                    subtitle = "Кім? Қайда? Не істеп жатыр?",
                    emoji = "🖼️",
                    minutes = 5,
                    voice("l5c1", "Сурет туралы толық сөйлем айт", "Бала саябақта қызыл доппен ойнап жүр.", "🧒🌳🔴", "Нағыз шешен екенсің! 🌟"),
                    voice("l5c2", "Сурет туралы толық сөйлем айт", "Әже ас үйде дәмді сорпа пісіріп жатыр.", "👵🍲", "Тілің қандай тәтті! 🍲"),
                    voice("l5c3", "Сурет туралы толық сөйлем айт", "Екі дос жаңбырда бір қолшатыр ұстап тұр.", "🧒☔🧒", "Қайырымды бала бол! ☔"),
                    voice("l5c4", "Сурет туралы толық сөйлем айт", "Кішкентай күшік бақта көбелекті қуып жүр.", "🐶🦋🌷", "Өте әдемі әңгіме! 🦋"),
                ),
            ),
        ),
        CurriculumLevel(
            id = 6,
            title = "Алтын Бәйтерек",
            subtitle = "Күрделі нұсқау, ұзақ сөйлем және толық әңгіме",
            focus = "2–3 қадам • 5–7 сөз • өздігінен әңгімелеу",
            emoji = "🌟",
            imageUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&q=80&w=1000",
            vocabulary = listOf(
                VocabularyWord("v21", "Бәйтерек", "🌟", "Қазақстанның символы.", "Бәйтерек — биік мұнара."),
                VocabularyWord("v22", "Кемпірқосақ", "🌈", "Жаңбырдан кейінгі жеті түс.", "Аспанда кемпірқосақ көрінді."),
                VocabularyWord("v23", "Сыйлық", "🎁", "Қуаныш үшін берілетін зат.", "Маған үлкен сыйлық берді."),
                VocabularyWord("v24", "Аспан", "☁️", "Жердің үстіндегі көк кеңістік.", "Аспанда күн жарқырап тұр."),
                VocabularyWord("v6a", "Жұлдыз", "⭐", "Түнде аспанда жанатын нүкте.", "Түнде аспанда жұлдыздар көп болады."),
                VocabularyWord("v6b", "Ту", "🇰🇿", "Еліміздің рәмізі.", "Көк туымыз желбіреп тұр."),
                VocabularyWord("v6c", "Жорық", "🧗", "Саяхатқа шығу.", "Біз бірге қызықты жорыққа шықтық."),
                VocabularyWord("v6d", "Мереке", "🥳", "Қуанышты атаулы күн.", "Бүгін бізде үлкен мереке."),
                VocabularyWord("v6e", "Домбыра", "🪕", "Қазақтың ұлттық аспабы.", "Атам домбырамен күй тартады.")
            ),
            activities = listOf(
                activity(
                    id = "l6_directions",
                    title = "Зейінді тыңдаушы",
                    subtitle = "Екі және үш қадамды есте сақта",
                    emoji = "🧠",
                    minutes = 6,
                    choice("l6a1", "Ретті дәл таңда", "Алдымен алмаға, sодан кейін бананға баs.", "Қай рет дұрыс?", "🍎 → 🍌", "🍌 → 🍎", "🍎 → 🍌", "🍐 → 🍎", hint = "Алдымен — бірінші, sодан кейін — екінші."),
                    choice("l6a2", "Ретті дәл таңда", "Алдымен қолыңды жу, кейін алманы же.", "Қай рет дұрыс?", "🧼 → 🍎", "🍎 → 🧼", "🧼 → 🍎", "🛏️ → 🍎", hint = "Тамақтан бұрын қол жуамыз."),
                    choice("l6a3", "Үш қадамды еsте sақта", "Допты ал, sебетке sал, sодан кейін қол шапалақта.", "Қай рет дұрыс?", "⚽ → 🧺 → 👏", "👏 → ⚽ → 🧺", "⚽ → 🧺 → 👏", "🧺 → 👏 → ⚽", hint = "Сөйлемді үш кішкентай бөлікке бөліп тыңда."),
                    choice("l6a4", "Белгілерді түгел тыңда", "Үлкен қызыл доптың жанындағы кішкентай көк допты таңда.", "Қай доп?", "🔵 Кішкентай көк доп", "🔴 Үлкен қызыл доп", "🔵 Кішкентай көк доп", "🟡 Үлкен сары доп", hint = "Кішкентай, көк және жанында деген үш белгіні еsте sақта."),
                    choice("l6a5", "Үш қадамды еsте sақта", "Кітапты аш, sуретті тап, sоңында оқиғаны айтып бер.", "Қай рет дұрыс?", "📖 → 🖼️ → 🗣️", "🗣️ → 📖 → 🖼️", "📖 → 🖼️ → 🗣️", "🖼️ → 📖 → 🗣️", hint = "Аш — тап — айтып бер."),
                ),
                activity(
                    id = "l6_sentences",
                    title = "Шебер сөйлем",
                    subtitle = "Ұзын сөйлемді дұрыс құрастыр",
                    emoji = "✨",
                    minutes = 6,
                    arrange("l6b1", "Сөйлемді құрастыр", "Бала тәтті қызыл алма жеді.", "Бала", "тәтті", "қызыл", "алма", "жеді."),
                    arrange("l6b2", "Сөйлемді құрастыр", "Ақ мысық жылы sүт ішті.", "Ақ", "мысық", "жылы", "sүт", "ішті."),
                    arrange("l6b3", "Сөйлемді құрастыр", "Ағам үлкен көк допты қатты тепті.", "Ағам", "үлкен", "көк", "допты", "қатты", "тепті."),
                    arrange("l6b4", "Сөйлемді құрастыр", "Әдемі көбелек sары гүлге жай қонды.", "Әдемі", "көбелек", "sары", "гүлге", "жай", "қонды."),
                    arrange("l6b5", "Сөйлемді құрастыр", "Балалар жаңбыр тоқтағаннан кейін аулада ойнады.", "Балалар", "жаңбыр", "тоқтағаннан", "кейін", "аулада", "ойнады."),
                ),
                activity(
                    id = "l6_story",
                    title = "Үлкен әңгіме",
                    subtitle = "Оқиғаны реттеп, өз sөзіңмен айтып бер",
                    emoji = "🌟",
                    minutes = 7,
                    story("l6c1", "Гүл туралы әңгімені ретте", "Тұқым екті, күнде sу құйды, гүл өsкенде анаsына sыйлады.", StoryCard("🌰", "Тұқым екті"), StoryCard("💧", "Күнде су құйды"), StoryCard("🌷", "Гүл өсті"), StoryCard("🎁", "Анасына сыйлады")),
                    voice("l6c2", "Оқиғаны өз сөзіңмен айтып бер", "Алдымен бала тұқым екті. Күнде су құйды. Гүл өскенде оны анасына сыйлады.", "🌰💧🌷🎁", "Сенің әңгімең өте қызықты! 🌷"),
                    story("l6c3", "Күтпеген жаңбыр оқиғаsын ретте", "Балалар ойнады, жаңбыр баsталды, олар күркеге тығылды, жаңбыр тоқтағанда кемпірқоsақ көрді.", StoryCard("⚽", "Ойнап жүрді"), StoryCard("🌧️", "Жаңбыр басталды"), StoryCard("🏠", "Күркеге кірді"), StoryCard("🌈", "Кемпірқосақ көрді")),
                    voice("l6c4", "Оқиғаға сезім мен себеп қос", "Жаңбыр басталғанда балалар таңғалды. Олар құрғақ болу үшін күркеге кірді. Кемпірқосақты көріп қуанды.", "🌧️😮🏠🌈😊", "Сезімдерді өте жақсы жеткіздің! 😊"),
                    voice("l6c5", "Өз оқиғаңды ойлап тап", "Бір күні кішкентай күшік бақтан жұмбақ қорап тапты...", "🐶🌳📦", "Сенің қиялың өте ұшқыр! 📦"),
                ),
            ),
        ),
    )

    val allActivities: List<GameActivity> = levels.flatMap { it.activities }

    fun level(id: Int): CurriculumLevel = levels.first { it.id == id }

    fun activity(id: String): GameActivity = allActivities.first { it.id == id }

    fun levelForActivity(activityId: String): CurriculumLevel =
        levels.first { level -> level.activities.any { it.id == activityId } }

    fun nextActivity(activityId: String): GameActivity? {
        val index = allActivities.indexOfFirst { it.id == activityId }
        return allActivities.getOrNull(index + 1)
    }

    private fun activity(
        id: String,
        title: String,
        subtitle: String,
        emoji: String,
        minutes: Int,
        vararg rounds: ExerciseRound,
    ) = GameActivity(id, title, subtitle, emoji, minutes, rounds.toList(), "")

    private fun choice(
        id: String,
        instruction: String,
        speech: String,
        display: String,
        correct: String,
        vararg options: String,
        hint: String,
    ): ChoiceRound {
        val optionList = options.toList()
        return ChoiceRound(
            id = id,
            instruction = instruction,
            speech = speech,
            display = display,
            options = optionList,
            correctIndex = optionList.indexOf(correct).also { require(it >= 0) },
            hint = hint,
        )
    }

    private fun taps(
        id: String,
        instruction: String,
        word: String,
        emoji: String,
        correctTaps: Int,
        hint: String,
    ) = TapCountRound(id, instruction, word, emoji, correctTaps, hint)

    private fun arrange(
        id: String,
        instruction: String,
        speech: String,
        vararg words: String,
    ) = ArrangeWordsRound(
        id = id,
        instruction = instruction,
        speech = speech,
        wordsInOrder = words.toList(),
        hint = "Кім немесе не туралы айтылғанын бірінші қойып көр.",
    )

    private fun story(
        id: String,
        instruction: String,
        speech: String,
        vararg cards: StoryCard,
    ) = StoryOrderRound(
        id = id,
        instruction = instruction,
        speech = speech,
        cardsInOrder = cards.toList(),
        hint = "Алдымен не болды? Содан кейін ше? Соңында не болды?",
    )

    private fun voice(
        id: String,
        instruction: String,
        modelText: String,
        picture: String,
        childEncouragement: String,
    ) = VoiceRound(id, instruction, modelText, picture, childEncouragement)
}

fun scoreSpokenPhrase(expected: String, actual: String): Int {
    val expectedWords = normalizeWords(expected)
    val actualWords = normalizeWords(actual)
    if (expectedWords.isEmpty() || actualWords.isEmpty()) return 0
    if (expectedWords.joinToString(" ") == actualWords.joinToString(" ")) return 100

    val matches = expectedWords.count { expectedWord ->
        actualWords.any { actualWord ->
            actualWord == expectedWord ||
                (expectedWord.length >= 4 && actualWord.length >= 4 &&
                    expectedWord.take(3) == actualWord.take(3))
        }
    }
    return ((matches.toFloat() / expectedWords.size) * 100).toInt()
}

private fun normalizeWords(text: String): List<String> = text
    .lowercase()
    .replace(Regex("[^а-яәіңғүұқөһa-z0-9 ]"), " ")
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
