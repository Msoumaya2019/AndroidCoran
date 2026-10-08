# Parité fonctionnelle AndroidCoran

Référence immuable : `f538ae37565abf70032e7e215fe57c8b96c2152f` (branche main). Les autres branches ne sont pas fusionnées automatiquement.

Statuts autorisés : Non commencé, En développement, Implémenté, Testé, Validé, Bloqué. Un symbole inventorié ne prouve pas sa validation fonctionnelle. Les étapes nécessitant un compte et un appareil restent non validées.


## Matrice des parcours et limitations actuelles

Cette matrice est prioritaire pour le niveau de parité ; l’inventaire exhaustif de symboles ci-dessous distingue les portages restant à faire. **La mission n’est pas terminée.**

| Fonction / rôle | Source originale | Supabase / ressources | Android natif | Développement | Test |
|---|---|---|---|---|---|
| Lecture Hafs : 6236 versets, 114 sourates, pages/juz/hizb | src/core/quran.ts | Local | domain/Quran.kt | Testé | Testé (JVM + assets Android) |
| Navigation sourates, juz, hizb et recherche | src/ui/MainScreens.tsx; src/SurahPicker.tsx | Local | ui/CoranApp.kt:QuranScreen | Implémenté | Non commencé |
| Mushaf traditionnel en images, sélection, proportions | src/MushafPage.tsx; assets/mushaf | Local | ui/MushafView.kt | En développement | Testé (assets uniquement) |
| Mushaf Tajwid en images | src/MushafPage.tsx; assets/mushaf-tajweed | Local | ui/MushafView.kt | En développement | Non commencé |
| Mushaf QPC en glyphes, mots et lignes originaux | src/coranTest/CoranTestScreen.tsx; html.ts | Local / qcf-v4-page | ui/MushafView.kt + fonts TTF | En développement | Testé (marges avec les polices natives sur pages 1, 2, 100, 582, 604 ; fidélité exhaustive non validée) |
| Tajwid simplifié et couleurs des règles | src/core/readerData.ts; src/MushafPage.tsx | Local | ui/TajwidReader.kt | Implémenté | Non commencé |
| Traduction Rachid Maach | src/core/readerData.ts; src/data/translation-fr-rashid.json | Local | Quran.french + ReaderScreen | Implémenté (texte et notes originales ; sélection native) | Testé (texte et notes JVM ; parcours appareil à valider) |
| Coran 1441 : téléchargement reprenable et 15 lignes/page | src/services/quranDownload.ts; quranSources.ts | Archive externe originale | data/QuranDownloadWorker.kt + ui/ReaderAssets.kt | En développement | Non commencé (archive réseau non téléchargée) |
| Zoom, gestes RTL et sélection de verset | src/ui/ZoomableReader.tsx; src/core/pageNavigation.ts | Local | ui/MushafView.kt | Implémenté | Non commencé |
| Marques-pages et suppression conservant les tombstones | src/core/bookmarks.ts; BookmarksScreen.tsx | user_state.data.bookmarks | ReaderScreen + BookmarkScreen | Implémenté (dates, ordre et tombstones) | Testé (règles JVM ; synchronisation Bloqué) |
| Reprise et suivi audio du verset | src/App.tsx; coranTest/model.ts | user_state.data.lastRead / reader | ReaderScreen + RecitationService + AudioProgress ; temps, fraction et index du passage | En développement | Testé (calcul JVM et index Media3) ; suivi de sourate à valider |
| Sept récitateurs et résolution URL | src/core/audio.ts | CDN Islamic Network / EveryAyah | domain/AudioRules.kt | Implémenté | Non commencé (tous les récitateurs) |
| Répétitions, pauses, vitesse, nombre personnalisé et arrêt automatique | src/core/audio.ts; PassageAudioPlayer.tsx | Local / fichiers audio | AudioRules + RepeatPreferences + RepeatPause + ReaderAudioDialog + RecitationService ; paramètres locaux DataStore | En développement | Testé (JVM, formulaire Compose, réglages pendant la pause, vitesse Media3 et reprise des répétitions) |
| Lecteur développé, réduit et masqué | src/PassageAudioPlayer.tsx; src/App.tsx | Local | ReaderAudioControls ; pied de page Compose ; déplacement libre à porter | En développement | Testé (Compose, commandes et restauration SavedState) |
| Lecture continue par timestamps de sourate | src/services/quranAudioTimeline.ts | API Quran.com identique à la source | audio/ChapterAudio.kt | En développement | Non commencé |
| Arrière-plan, audio focus et commandes système | src/services/audioFocus.ts; PassageAudioPlayer.tsx | Local | Media3 MediaSessionService | En développement | Non commencé (interruptions) |
| Cache audio et préchargement des versets | src/services/verseAudioCache.ts; PassageAudioPlayer.tsx | Local | Cache Media3 borné à 250 Mo ; fichiers publics uniquement ; préchargement des trois versets suivants borné au passage. La source ne propose pas de téléchargement audio explicite ni de reprise du lecteur après arrêt du processus | Implémenté | Testé (cache Android et relecture complète sans source réseau) |
| Objectifs et rythmes ; ordre depuis An Nâs | src/core/program.ts; ui/GoalScreen.tsx | user_state.data.goal / pace | domain/Program.kt + GoalScreen | En développement (sourates/hizb/juz/plages disponibles ; parcours complet à valider) | Testé (ordre et jours JVM) |
| Connaissances initiales et validation par verset | src/App.tsx; core/program.ts | user_state.data.knowledge | GoalScreen + Program.markKnowledge | Implémenté (sourates/hizb/juz/plages et niveaux acquis/révision/apprentissage) | Testé (mutations JVM) |
| Création, report et régénération du programme | src/core/program.ts | user_state.data.sessions | Program.generate + ProgramScreen | En développement (extension legacy à porter) | Testé (JVM) |
| Validation partielle et reprise exacte des séances | src/core/studyProgress.ts | user_state.data.studyProgress / memorizedAt | Program.complete + ReaderScreen | En développement | Testé (JVM, persistance à compléter) |
| Révisions pondérées ; cycles et unités réelles | src/core/review.ts | user_state.data.reviewCycle | domain/Review.kt | En développement | Testé (partition JVM) |
| Consolidations J+1/J+3/J+7 et priorités | src/core/review.ts | reviewConsolidations / difficultyMarkers | Review.prepare / grade / tasks | En développement (actions explicites à compléter) | Testé (consolidation JVM) |
| Révisions partielles, historique et notes | src/core/review.ts; ReviewDashboard.tsx | reviewHistory / studyProgress | Review.grade + RevisionScreen | En développement | Non commencé (parcours complet) |
| Statistiques volume / progrès / hizb | src/core/program.ts; weeklyProgress.ts | user_state.data | LearningScreens.kt + Statistics.kt ; jour/semaine/mois, historique et séries | Implémenté | Testé (calculs JVM) ; parcours visuel Non commencé |
| Auth, comptes existants, inscription, session | src/services/sync.ts; authStorage.ts | Supabase Auth existant | data/Repository.kt + AccountScreen | En développement | Testé (lectures publiques SDK Android sans activité) ; compte existant/RLS Bloqué |
| Réinitialisation, liens et changement de mot de passe | src/services/sync.ts | Supabase Auth | Repository + MainActivity + AccountScreen ; liens implicites existants, récupération, renvoi de confirmation et mot de passe | Implémenté | Testé (contrats JVM et Intent Android réussi) ; session réelle Bloqué |
| Synchronisation, JSON compatible et isolation des comptes | src/services/sync.ts; storage.ts | user_state | Repository + LocalStore (SQLite natif) | En développement | Testé (isolation locale Android ; Supabase Bloqué) |
| Fusion offline complète et files idempotentes | src/core/offlineMerge.ts; services/offlineSync.ts | user_state + queues | domain/OfflineMerge.kt + Repository + OutboxWorker ; quiz et signalements persistés par compte, autres services à compléter | En développement | Testé (fusion JVM) ; serveur Bloqué |
| Amis, codes, demandes, liste et groupes | src/SocialScreens.tsx; services/social.ts | friend_profiles / friend_links / friend_groups + RPC | data/SocialService.kt + ui/SocialScreens.kt ; invitations et rôles de groupe | En développement | Bloqué — backend non connecté |
| Messagerie texte privée et de groupe | src/services/social.ts | friend_messages | ConversationScreen ; pages de 50, masquage, lectures et Realtime au premier plan | Implémenté | Testé (contrats JVM) ; serveur Bloqué |
| Avatars, présence, recherche, blocage et modération | src/services/social.ts; avatars.ts | friend_profiles / storage + RPC | SocialScreens + Avatars + AvatarEditor ; profils, photo, présence, blocage, signalement et rôles ; staging photo avant inscription et rapports de groupe à compléter | En développement | Bloqué — serveur ; sélection photo et orientation à valider |
| Objectifs partagés et rendez-vous de révision | src/services/social.ts | friend_shared_goals / friend_review_appointments | SocialService + ConversationDetails ; calendrier et heure natifs | Implémenté | Bloqué — backend non connecté |
| Enregistrement natif Coran et sauvegarde offline | src/RecitationRecorder.tsx; services/recitations.ts | Local ; bucket recitations ; table recitations | RecorderPanel + recordingPayload ; Coran et invocations avec snapshot original | En développement | Testé (payloads JVM) ; microphone Non commencé |
| Liste, lecture et consultation de corrections | src/RecitationsScreen.tsx; services/recitations.ts | recitations / recitation_corrections | RecitationsScreen + Media3 ; invocations, retours généraux et voix, écoute locale | En développement | Bloqué — serveur ; interface compilée |
| Partage de récitations et retours vocaux | src/services/recitations.ts; social.ts | recitation_feedback / friend_messages / bucket | ConversationScreen + RecitationsScreen + AdminVoicePanel ; partage à compléter | En développement | Bloqué — serveur |
| Quiz quotidien, réponses et défis 5/10 questions | src/ui/QuizScreen.tsx; services/quiz.ts | quiz_snapshot / quiz_answer_daily / quiz_create_challenge / quiz_answer_challenge | QuizScreens.kt + QuizService + Quiz.kt ; réponses offline et défis | En développement | Testé (règles JVM) ; serveur Bloqué |
| Quiz thématiques, historique, statistiques et notifications | src/core/quiz.ts; services/quiz.ts | quiz_sets + RPC | QuizScreens.kt ; choix thématique, historique, scores et préférences | En développement | Testé (scores JVM) ; serveur Bloqué |
| Contenus quotidiens, rappels et invocations | src/DailyContentsScreen.tsx; services/dailyContents.ts | daily_contents / daily_content_for_date | ContentService + ContentScreens ; catégories, favoris, pagination, image, audio et enregistrement invocation | En développement | Bloqué — backend ; médias sur appareil Non commencé |
| Thèmes, accents, polices et papier | src/ui/AppearanceScreen.tsx; theme/* | user_state.data | NativeAppTheme + AppearanceOptions ; palettes originales, quatre accents, trois modes de police et papier QCF | Implémenté | Testé (règles JVM) ; fidélité exhaustive Non commencé |
| Rappel local à 19 h, redémarrage et préférences | src/services/notifications.ts | user_state.data.notifications | LocalReminders + ReminderSettings | En développement | Non commencé (déclenchement/boot) |
| Push messages, amis, corrections, admin et quiz | src/services/notifications.ts; supabase/*notifications* | Jetons Expo et RPC existants | Canal FCM additif proposé dans docs/FCM_PROPOSAL.md | Bloqué | Bloqué |
| Signalement bug | src/ui/ProblemReport.tsx; services/problemReports.ts | app_problem_reports / screenshot bucket | ProblemReportScreen + ProblemReportService + OutboxWorker ; pièce jointe et reprise réseau | En développement | Testé (persistance Android) ; serveur Bloqué |
| Contact administrateur | src/SocialScreens.tsx; supabase/admin-contact.sql | groupes administrateur et messages | FriendsScreen → open_admin_contact → conversation de groupe native | Implémenté | Bloqué — backend non connecté |
| Administration comptes, quiz et quiz sets | src/AdminAccounts.tsx; ui/AdminQuiz*.tsx | admin_learning_accounts / quiz_admin_* | AdminService + AdminScreen + AdminQuizPanel : comptes, questions et séries de dix questions | En développement | Bloqué — serveur non connecté |
| Administration récitations, corrections et rapports | src/AdminRecitations.tsx; ui/AdminProblemReports.tsx | recitations / app_problem_reports / RPC | AdminRecitationsPanel + AdminVoicePanel + AdminReportsPanel : voix, finalisation, captures et résolution | En développement | Testé (contrats JVM) ; serveur et microphone Bloqué/Non commencé |
| Administration contenus, catégories et calendrier | src/AdminDailyContents.tsx | daily_contents / content_categories / daily_content_schedule | AdminContentsPanel + ContentMedia : édition, catégories, programmation, médias et nettoyage des fichiers inutilisés | En développement | Testé (contrats JVM) ; serveur Bloqué |
| Administration notifications et historique | src/AdminNotifications.tsx | send_admin_notification / admin_notifications | AdminNotificationsPanel : rédaction, destinataires, confirmation, idempotence et historique | En développement | Bloqué — serveur non connecté |
| CI : Gradle, tests, lint et APK debug | Nouveau dépôt seulement | Aucune |  .github/workflows/android.yml | Implémenté | Testé — run public 37761443828 réussi, APK et rapports publiés |

| Source | Fonction / rôle à vérifier | Supabase | Équivalent natif prévu | Développement | Test |
|---|---|---|---|---|---|
| src/AdminAccounts.tsx:7 | AdminAccounts | Local / via services | ui/AdminScreen | Implémenté | Bloqué — serveur |
| src/AdminDailyContents.tsx:9 | AdminDailyContents | Local / via services | ui/AdminContentsPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/AdminNotifications.tsx:8 | AdminNotifications | Local / via services | ui/AdminNotificationsPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/AdminRecitations.tsx:11 | AdminRecitations | Local / via services | ui/AdminRecitationsPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/AdminVoiceRecorder.tsx:8 | AdminVoiceRecorder | from:recitations | ui/AdminVoicePanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/BookmarksScreen.tsx:9 | BookmarksScreen | Local / via services | ui/BookmarksScreen | Non commencé | Non commencé |
| src/coranTest/CoranTestScreen.tsx:8 | CoranTestScreen | Local / via services | ui/CoranTestScreen | Non commencé | Non commencé |
| src/coranTest/html.ts:7 | testPageHtml | Local / via services | ui/testPageHtml | Non commencé | Non commencé |
| src/coranTest/loadPage.ts:14 | loadTestPage | Local / via services | ui/loadTestPage | Non commencé | Non commencé |
| src/coranTest/loadPage.web.ts:12 | loadTestPage | Local / via services | ui/loadTestPage | Non commencé | Non commencé |
| src/coranTest/model.ts:12 | originalPageWidth | Local / via services | ui/originalPageWidth | Non commencé | Non commencé |
| src/coranTest/model.ts:13 | verseIndex | Local / via services | ui/verseIndex | Non commencé | Non commencé |
| src/coranTest/model.ts:16 | testPageRange | Local / via services | ui/testPageRange | Non commencé | Non commencé |
| src/coranTest/model.ts:17 | validTestPage | Local / via services | ui/validTestPage | Non commencé | Non commencé |
| src/coranTest/model.ts:18 | adjacentTestPages | Local / via services | ui/adjacentTestPages | Non commencé | Non commencé |
| src/coranTest/model.ts:22 | readerOverlayState | Local / via services | ui/readerOverlayState | Non commencé | Non commencé |
| src/coranTest/model.ts:25 | testVersePage | Local / via services | ui/testVersePage | Non commencé | Non commencé |
| src/coranTest/model.ts:29 | verseRegions | Local / via services | ui/verseRegions | Non commencé | Non commencé |
| src/coranTest/model.ts:43 | emptyPlaybackState | Local / via services | ui/emptyPlaybackState | Non commencé | Non commencé |
| src/coranTest/model.ts:46 | testAudioAdapter | Local / via services | ui/testAudioAdapter | Non commencé | Non commencé |
| src/coranTest/PageSurface.tsx:5 | PageSurface | Local / via services | ui/PageSurface | Non commencé | Non commencé |
| src/coranTest/PageSurface.web.tsx:3 | PageSurface | Local / via services | ui/PageSurface | Non commencé | Non commencé |
| src/coranTest/resources.ts:1214 | titleFont | Local / via services | ui/titleFont | Non commencé | Non commencé |
| src/coranTest/resources.ts:1215 | arabicFont | Local / via services | ui/arabicFont | Non commencé | Non commencé |
| src/coranTest/resources.ts:1216 | basmalaFont | Local / via services | ui/basmalaFont | Non commencé | Non commencé |
| src/core/audio.ts:4 | reciters | Local / via services | domain/AudioRules.kt : reciters | Implémenté | Non commencé (tous les récitateurs) |
| src/core/audio.ts:13 | defaultReciter | Local / via services | domain/AudioRules.kt : reciters[3] | Implémenté | Testé (préférences JVM) |
| src/core/audio.ts:14 | DEFAULT_AYAH_GAP_MS | Local / via services | domain/RepeatPreferences.kt : TECHNICAL_AYAH_GAP_MS | Implémenté | Testé (JVM) |
| src/core/audio.ts:20 | parseChapterAudio | Local / via services | audio/ChapterAudio.kt : parse | Implémenté | Non commencé (timestamps distants) |
| src/core/audio.ts:32 | continuousAudioPosition | Local / via services | audio/RecitationService.kt : follow | Implémenté | Non commencé (lecture de sourate) |
| src/core/audio.ts:38 | verseAudioUrl | Local / via services | domain/AudioRules.kt : verseAudioUrl | Implémenté | Testé (Alafasy Android) |
| src/core/audio.ts:45 | resolveAudioSegment | Local / via services | audio/RecitationService.kt : playVerse | Implémenté | Testé (Alafasy Android) |
| src/core/audio.ts:50 | audioRange | Local / via services | domain/VerseRange + ReaderAudioDialog.selectedRange | Implémenté | Testé (JVM et Compose) |
| src/core/audio.ts:55 | nextAudioPosition | Local / via services | domain/AudioRules.kt : nextAudioPosition | Implémenté | Testé (JVM et répétitions Android) |
| src/core/audio.ts:71 | verseAudioLabel | Local / via services | ui/ReaderScreen : sourate/verset | Implémenté | Non commencé (libellé dédié) |
| src/core/ayahMarker.ts:2 | easternArabicNumber | Local / via services | ui/easternArabicNumber | Non commencé | Non commencé |
| src/core/ayahMarker.ts:7 | ayahMarkerHtml | Local / via services | ui/ayahMarkerHtml | Non commencé | Non commencé |
| src/core/bookmarks.ts:5 | saveBookmark | Local / via services | domain/Bookmarks.kt | Implémenté | Testé (JVM) |
| src/core/bookmarks.ts:10 | deleteBookmark | Local / via services | domain/Bookmarks.kt | Implémenté | Testé (JVM) |
| src/core/bookmarks.ts:14 | useBookmark | Local / via services | domain/Bookmarks.kt | Implémenté | Testé (JVM) |
| src/core/bookmarks.ts:18 | visibleBookmarks | Local / via services | domain/Bookmarks.kt | Implémenté | Testé (JVM) |
| src/core/bookmarks.ts:20 | mergeBookmarks | Local / via services | domain/Bookmarks.kt | Implémenté | Testé (JVM) |
| src/core/marginAnnotations.ts:4 | marginAnnotations | Local / via services | ui/marginAnnotations | Non commencé | Non commencé |
| src/core/offlineAccess.ts:2 | initialAccountAccess | Local / via services | ui/initialAccountAccess | Non commencé | Non commencé |
| src/core/offlineMerge.ts:30 | mergeOfflineState | user_state | domain/OfflineMerge.kt | Implémenté | Testé (JVM ; serveur Bloqué) |
| src/core/offlineQueue.ts:1 | createSyncWorker | Local / via services | ui/createSyncWorker | Non commencé | Non commencé |
| src/core/pageNavigation.ts:1 | pageAfterSwipe | Local / via services | ui/pageAfterSwipe | Non commencé | Non commencé |
| src/core/program.ts:41 | availablePaces | Local / via services | domain/availablePaces | Non commencé | Non commencé |
| src/core/program.ts:42 | weekdays | Local / via services | domain/weekdays | Non commencé | Non commencé |
| src/core/program.ts:46 | goalFromPreset | Local / via services | domain/goalFromPreset | Non commencé | Non commencé |
| src/core/program.ts:57 | defaultState | Local / via services | domain/defaultState | Non commencé | Non commencé |
| src/core/program.ts:59 | migrateReaderState | Local / via services | domain/migrateReaderState | Non commencé | Non commencé |
| src/core/program.ts:65 | reconcileState | Local / via services | domain/reconcileState | Non commencé | Non commencé |
| src/core/program.ts:82 | accountState | Local / via services | domain/accountState | Non commencé | Non commencé |
| src/core/program.ts:90 | resetAllProgress | Local / via services | domain/resetAllProgress | Non commencé | Non commencé |
| src/core/program.ts:95 | todayLocal | Local / via services | domain/todayLocal | Non commencé | Non commencé |
| src/core/program.ts:96 | dateKey | Local / via services | domain/dateKey | Non commencé | Non commencé |
| src/core/program.ts:97 | addDays | Local / via services | domain/addDays | Non commencé | Non commencé |
| src/core/program.ts:98 | dayOf | Local / via services | domain/dayOf | Non commencé | Non commencé |
| src/core/program.ts:99 | touch | Local / via services | domain/touch | Non commencé | Non commencé |
| src/core/program.ts:101 | markKnowledge | Local / via services | domain/markKnowledge | Non commencé | Non commencé |
| src/core/program.ts:110 | isRangeKnown | Local / via services | domain/isRangeKnown | Non commencé | Non commencé |
| src/core/program.ts:114 | goalIsAlreadyKnown | Local / via services | domain/goalIsAlreadyKnown | Non commencé | Non commencé |
| src/core/program.ts:115 | toggleKnownRange | Local / via services | domain/toggleKnownRange | Non commencé | Non commencé |
| src/core/program.ts:118 | partialKnownRanges | Local / via services | domain/partialKnownRanges | Non commencé | Non commencé |
| src/core/program.ts:131 | goalIds | Local / via services | domain/goalIds | Non commencé | Non commencé |
| src/core/program.ts:132 | learningOrderIds | Local / via services | domain/learningOrderIds | Non commencé | Non commencé |
| src/core/program.ts:138 | memorizedIds | Local / via services | domain/memorizedIds | Non commencé | Non commencé |
| src/core/program.ts:139 | progress | Local / via services | domain/progress | Non commencé | Non commencé |
| src/core/program.ts:147 | validGoal | Local / via services | domain/validGoal | Non commencé | Non commencé |
| src/core/program.ts:190 | generateProgram | Local / via services | domain/generateProgram | Non commencé | Non commencé |
| src/core/program.ts:208 | seedInitialRevisions | Local / via services | domain/seedInitialRevisions | Non commencé | Non commencé |
| src/core/program.ts:215 | postponeSession | Local / via services | domain/postponeSession | Non commencé | Non commencé |
| src/core/program.ts:220 | completeSession | Local / via services | domain/completeSession | Non commencé | Non commencé |
| src/core/program.ts:234 | extendLearningProgram | Local / via services | domain/extendLearningProgram | Non commencé | Non commencé |
| src/core/program.ts:243 | gradeRevision | Local / via services | domain/gradeRevision | Non commencé | Non commencé |
| src/core/program.ts:257 | completedHizbs | Local / via services | domain/completedHizbs | Non commencé | Non commencé |
| src/core/program.ts:258 | stats | Local / via services | domain/stats | Non commencé | Non commencé |
| src/core/quiz.ts:1 | quizCategories | Local / via services | ui/quizCategories | Non commencé | Non commencé |
| src/core/quiz.ts:7 | emptyQuiz | Local / via services | domain/Quiz.kt | Implémenté | Testé (JVM) |
| src/core/quiz.ts:8 | quizDay | Local / via services | ui/quizDay | Non commencé | Non commencé |
| src/core/quiz.ts:9 | challengeStatus | Local / via services | ui/challengeStatus | Non commencé | Non commencé |
| src/core/quiz.ts:14 | quizStatistics | Local / via services | domain/Quiz.kt | Implémenté | Testé (JVM) |
| src/core/quiz.ts:15 | mergeQuizSnapshot | Local / via services | domain/Quiz.kt | Implémenté | Testé (JVM) |
| src/core/quiz.ts:16 | recordDailyAnswer | Local / via services | domain/Quiz.kt | Implémenté | Testé (JVM) |
| src/core/quran.ts:9 | verses | Local / via services | domain/verses | Non commencé | Non commencé |
| src/core/quran.ts:10 | surahs | Local / via services | domain/surahs | Non commencé | Non commencé |
| src/core/quran.ts:11 | juzs | Local / via services | domain/juzs | Non commencé | Non commencé |
| src/core/quran.ts:12 | quarters | Local / via services | domain/quarters | Non commencé | Non commencé |
| src/core/quran.ts:13 | pages | Local / via services | domain/pages | Non commencé | Non commencé |
| src/core/quran.ts:17 | verseId | Local / via services | domain/verseId | Non commencé | Non commencé |
| src/core/quran.ts:21 | verseAt | Local / via services | domain/verseAt | Non commencé | Non commencé |
| src/core/quran.ts:22 | surahAt | Local / via services | domain/surahAt | Non commencé | Non commencé |
| src/core/quran.ts:23 | pageOf | Local / via services | domain/pageOf | Non commencé | Non commencé |
| src/core/quran.ts:37 | pageRange | Local / via services | domain/pageRange | Non commencé | Non commencé |
| src/core/quran.ts:42 | intersect | Local / via services | domain/intersect | Non commencé | Non commencé |
| src/core/quran.ts:46 | normalizeRanges | Local / via services | domain/normalizeRanges | Non commencé | Non commencé |
| src/core/quran.ts:56 | expand | Local / via services | domain/expand | Non commencé | Non commencé |
| src/core/quran.ts:61 | weights | Local / via services | domain/weights | Non commencé | Non commencé |
| src/core/quran.ts:62 | volume | Local / via services | domain/volume | Non commencé | Non commencé |
| src/core/quran.ts:63 | totalVolume | Local / via services | domain/totalVolume | Non commencé | Non commencé |
| src/core/quran.ts:64 | reference | Local / via services | domain/reference | Non commencé | Non commencé |
| src/core/quranSources.ts:5 | zipSources | Local / via services | domain/zipSources | Non commencé | Non commencé |
| src/core/quranSources.ts:6 | isZipSource | Local / via services | domain/isZipSource | Non commencé | Non commencé |
| src/core/quranSources.ts:7 | zipPageData | Local / via services | domain/zipPageData | Non commencé | Non commencé |
| src/core/quranSources.ts:13 | zipVersePages | Local / via services | domain/zipVersePages | Non commencé | Non commencé |
| src/core/quranSources.ts:17 | zipVersePage | Local / via services | domain/zipVersePage | Non commencé | Non commencé |
| src/core/quranSources.ts:18 | zipPageRange | Local / via services | domain/zipPageRange | Non commencé | Non commencé |
| src/core/quranSources.ts:20 | zipVerseRegions | Local / via services | domain/zipVerseRegions | Non commencé | Non commencé |
| src/core/readerAppearance.ts:1 | quranPaperOptions | Local / via services | ui/quranPaperOptions | Non commencé | Non commencé |
| src/core/readerAppearance.ts:8 | quranPaperColor | Local / via services | ui/quranPaperColor | Non commencé | Non commencé |
| src/core/readerData.ts:14 | frenchVerse | Local / via services | ui/frenchVerse | Non commencé | Non commencé |
| src/core/readerData.ts:15 | tajweedVerse | Local / via services | ui/tajweedVerse | Non commencé | Non commencé |
| src/core/readerData.ts:20 | tajweedColor | Local / via services | ui/tajweedColor | Non commencé | Non commencé |
| src/core/readerData.ts:29 | tajweedSpans | Local / via services | ui/tajweedSpans | Non commencé | Non commencé |
| src/core/readerData.ts:42 | verseAtImagePoint | Local / via services | ui/verseAtImagePoint | Non commencé | Non commencé |
| src/core/readerLayout.ts:6 | fitMushafPage | Local / via services | ui/fitMushafPage | Non commencé | Non commencé |
| src/core/readerZoom.ts:3 | constrainReaderZoom | Local / via services | ui/constrainReaderZoom | Non commencé | Non commencé |
| src/core/readerZoom.ts:7 | zoomReaderAt | Local / via services | ui/zoomReaderAt | Non commencé | Non commencé |
| src/core/review.ts:6 | consolidationOffsets | Local / via services | domain/consolidationOffsets | Non commencé | Non commencé |
| src/core/review.ts:9 | reviewsEnabled | Local / via services | domain/reviewsEnabled | Non commencé | Non commencé |
| src/core/review.ts:10 | reviewCycleDays | Local / via services | domain/reviewCycleDays | Non commencé | Non commencé |
| src/core/review.ts:18 | reviewWeight | Local / via services | domain/reviewWeight | Non commencé | Non commencé |
| src/core/review.ts:23 | partitionReviewCorpus | Local / via services | domain/partitionReviewCorpus | Non commencé | Non commencé |
| src/core/review.ts:53 | setReviewsEnabled | Local / via services | domain/setReviewsEnabled | Non commencé | Non commencé |
| src/core/review.ts:57 | setReviewCycle | Local / via services | domain/setReviewCycle | Non commencé | Non commencé |
| src/core/review.ts:62 | toggleDifficulty | Local / via services | domain/toggleDifficulty | Non commencé | Non commencé |
| src/core/review.ts:79 | prepareReviewSchedule | Local / via services | domain/prepareReviewSchedule | Non commencé | Non commencé |
| src/core/review.ts:106 | reviewQuantity | Local / via services | domain/reviewQuantity | Non commencé | Non commencé |
| src/core/review.ts:116 | reviewPlan | Local / via services | domain/reviewPlan | Non commencé | Non commencé |
| src/core/review.ts:138 | gradeReviewTask | Local / via services | domain/gradeReviewTask | Non commencé | Non commencé |
| src/core/review.ts:160 | reviewRhythm | Local / via services | domain/reviewRhythm | Non commencé | Non commencé |
| src/core/review.ts:171 | partitionDailyQuantity | Local / via services | domain/partitionDailyQuantity | Non commencé | Non commencé |
| src/core/review.ts:176 | setReviewQuantity | Local / via services | domain/setReviewQuantity | Non commencé | Non commencé |
| src/core/review.ts:181 | completeConsolidation | Local / via services | domain/completeConsolidation | Non commencé | Non commencé |
| src/core/sourceNavigation.ts:4 | sourceVersePage | Local / via services | ui/sourceVersePage | Non commencé | Non commencé |
| src/core/sourceNavigation.ts:5 | sourcePageRange | Local / via services | ui/sourcePageRange | Non commencé | Non commencé |
| src/core/studyProgress.ts:7 | studyKey | Local / via services | domain/studyKey | Non commencé | Non commencé |
| src/core/studyProgress.ts:8 | studyPage | Local / via services | domain/studyPage | Non commencé | Non commencé |
| src/core/studyProgress.ts:9 | studyPageRange | Local / via services | domain/studyPageRange | Non commencé | Non commencé |
| src/core/studyProgress.ts:10 | studyLastPage | Local / via services | domain/studyLastPage | Non commencé | Non commencé |
| src/core/studyProgress.ts:11 | studyEndpointForPage | Local / via services | domain/studyEndpointForPage | Non commencé | Non commencé |
| src/core/studyProgress.ts:12 | studyMetrics | Local / via services | domain/studyMetrics | Non commencé | Non commencé |
| src/core/studyProgress.ts:20 | studyRangeLabel | Local / via services | domain/studyRangeLabel | Non commencé | Non commencé |
| src/core/studyProgress.ts:21 | studySurahs | Local / via services | domain/studySurahs | Non commencé | Non commencé |
| src/core/studyProgress.ts:22 | studyVerses | Local / via services | domain/studyVerses | Non commencé | Non commencé |
| src/core/studyProgress.ts:23 | remainingStudyRange | Local / via services | domain/remainingStudyRange | Non commencé | Non commencé |
| src/core/studyProgress.ts:24 | validateStudyProgress | Local / via services | domain/validateStudyProgress | Non commencé | Non commencé |
| src/core/studyProgress.ts:38 | resumeStudyTask | Local / via services | domain/resumeStudyTask | Non commencé | Non commencé |
| src/core/toumoun.ts:16 | toumounRecords | Local / via services | ui/toumounRecords | Non commencé | Non commencé |
| src/core/toumoun.ts:19 | verifiedToumounRanges | Local / via services | ui/verifiedToumounRanges | Non commencé | Non commencé |
| src/core/toumoun.ts:39 | verifiedToumouns | Local / via services | ui/verifiedToumouns | Non commencé | Non commencé |
| src/core/weeklyProgress.ts:2 | scheduledDate | Local / via services | ui/scheduledDate | Non commencé | Non commencé |
| src/core/weeklyProgress.ts:3 | upcomingSessions | Local / via services | ui/upcomingSessions | Non commencé | Non commencé |
| src/core/weeklyProgress.ts:7 | weeklyProgress | Local / via services | ui/weeklyProgress | Non commencé | Non commencé |
| src/core/weeklyProgress.ts:14 | sessionStatus | Local / via services | ui/sessionStatus | Non commencé | Non commencé |
| src/DailyContentsScreen.tsx:15 | ContentCard | Local / via services | ui/ContentCard | Non commencé | Non commencé |
| src/DailyContentsScreen.tsx:41 | ContentTabs | Local / via services | ui/ContentTabs | Non commencé | Non commencé |
| src/DailyContentsScreen.tsx:47 | TodayContents | Local / via services | ui/TodayContents | Non commencé | Non commencé |
| src/DailyContentsScreen.tsx:53 | DailyContentsScreen | Local / via services | ui/DailyContentsScreen | Non commencé | Non commencé |
| src/MushafPage.tsx:21 | MushafPage | Local / via services | ui/MushafPage | Non commencé | Non commencé |
| src/PassageAudioPlayer.tsx:22 | PassageAudioPlayer | Local / via services | ReaderAudioDialog + ReaderAudioControls + RecitationService | En développement | Testé (Compose et Media3 ; déplacement libre restant) |
| src/RecitationRecorder.tsx:18 | RecitationRecorder | Local / via services | ui/RecitationRecorder | Non commencé | Non commencé |
| src/RecitationsScreen.tsx:14 | RecitationsScreen | Local / via services | ui/RecitationsScreen | Non commencé | Non commencé |
| src/ReviewDashboard.tsx:11 | ReviewDashboard | Local / via services | ui/ReviewDashboard | Non commencé | Non commencé |
| src/services/adminAccounts.ts:5 | listLearningAccounts | rpc:admin_learning_accounts | data/AdminService.accounts | En développement | Bloqué — serveur |
| src/services/adminNotifications.ts:8 | listAdminNotificationRecipients | rpc:admin_notification_recipients, from:admin_notifications, rpc:send_admin_notification | ui/AdminNotificationsPanel | En développement | Bloqué — serveur |
| src/services/adminNotifications.ts:14 | listAdminNotificationHistory | rpc:admin_notification_recipients, from:admin_notifications, rpc:send_admin_notification | data/AdminService.notificationHistory | En développement | Bloqué — serveur |
| src/services/adminNotifications.ts:20 | sendAdminNotification | rpc:admin_notification_recipients, from:admin_notifications, rpc:send_admin_notification | ui/AdminNotificationsPanel + domain/adminNotificationPayload | En développement | Bloqué — serveur |
| src/services/audioFocus.ts:3 | stopActiveAudio | Local / via services | audio/stopActiveAudio | Non commencé | Non commencé |
| src/services/authStorage.ts:7 | authStorage | Local / via services | data/authStorage | Non commencé | Non commencé |
| src/services/avatars.ts:11 | chooseAvatar | rpc:ensure_social_profile, from:friend_profiles | data/chooseAvatar | Non commencé | Non commencé |
| src/services/avatars.ts:23 | stageAvatar | rpc:ensure_social_profile, from:friend_profiles | data/stageAvatar | Non commencé | Non commencé |
| src/services/avatars.ts:30 | uploadAvatar | rpc:ensure_social_profile, from:friend_profiles | data/uploadAvatar | Non commencé | Non commencé |
| src/services/avatars.ts:44 | syncStagedAvatar | rpc:ensure_social_profile, from:friend_profiles | data/syncStagedAvatar | Non commencé | Non commencé |
| src/services/avatars.ts:52 | removeAvatar | rpc:ensure_social_profile, from:friend_profiles | data/removeAvatar | Non commencé | Non commencé |
| src/services/avatars.ts:61 | avatarUrl | rpc:ensure_social_profile, from:friend_profiles | data/avatarUrl | Non commencé | Non commencé |
| src/services/connectivity.ts:4 | useConnectivity | Local / via services | data/useConnectivity | Non commencé | Non commencé |
| src/services/dailyContentMedia.ts:9 | resolveContentMedia | from:daily_contents | data/ContentMedia.resolve | En développement | Bloqué — serveur ; tests contrats JVM |
| src/services/dailyContentMedia.ts:15 | chooseAndUploadContentMedia | from:daily_contents | data/ContentMedia.upload | En développement | Bloqué — serveur ; tests contrats JVM |
| src/services/dailyContentMedia.ts:34 | removeContentMedia | from:daily_contents | data/removeContentMedia | Non commencé | Non commencé |
| src/services/dailyContentMedia.ts:35 | cleanUnusedContentMedia | from:daily_contents | data/ContentMedia.cleanUnused | En développement | Bloqué — serveur ; tests contrats JVM |
| src/services/dailyContents.ts:8 | localDate | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/localDate | Non commencé | Non commencé |
| src/services/dailyContents.ts:12 | contentChanged | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/contentChanged | Non commencé | Non commencé |
| src/services/dailyContents.ts:13 | observeContents | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/observeContents | Non commencé | Non commencé |
| src/services/dailyContents.ts:14 | dayContents | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/dayContents | Non commencé | Non commencé |
| src/services/dailyContents.ts:15 | cachedDayContents | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/cachedDayContents | Non commencé | Non commencé |
| src/services/dailyContents.ts:16 | categories | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/ContentService + ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:17 | contents | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/contents | Non commencé | Non commencé |
| src/services/dailyContents.ts:18 | saveContent | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:19 | deleteContent | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel + data/ContentMedia.cleanUnused | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:20 | saveCategory | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:21 | deleteCategory | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:22 | schedules | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:23 | scheduleContent | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:24 | removeSchedule | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/dailyContents.ts:26 | favorites | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/favorites | Non commencé | Non commencé |
| src/services/dailyContents.ts:32 | setFavorite | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/setFavorite | Non commencé | Non commencé |
| src/services/dailyContents.ts:33 | getContent | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/getContent | Non commencé | Non commencé |
| src/services/dailyContents.ts:35 | favoriteContents | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | data/favoriteContents | Non commencé | Non commencé |
| src/services/dailyContents.ts:37 | saveContentAndSchedule | rpc:daily_content_for_date, from:content_categories, from:daily_contents, from:daily_content_schedule, from:content_favorites, rpc:save_daily_content | ui/AdminContentsPanel | En développement | Bloqué — serveur |
| src/services/notifications.ts:41 | setActiveConversation | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/setActiveConversation | Non commencé | Non commencé |
| src/services/notifications.ts:42 | setMessagePresentationEnabled | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/setMessagePresentationEnabled | Non commencé | Non commencé |
| src/services/notifications.ts:43 | setProgressPresentationEnabled | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/setProgressPresentationEnabled | Non commencé | Non commencé |
| src/services/notifications.ts:44 | setCorrectionPresentationEnabled | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/setCorrectionPresentationEnabled | Non commencé | Non commencé |
| src/services/notifications.ts:45 | setAdminMessagePresentationEnabled | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/setAdminMessagePresentationEnabled | Non commencé | Non commencé |
| src/services/notifications.ts:46 | setRecitationsVisible | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/setRecitationsVisible | Non commencé | Non commencé |
| src/services/notifications.ts:48 | configureNotificationChannels | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/configureNotificationChannels | Non commencé | Non commencé |
| src/services/notifications.ts:56 | ensureNotificationPermission | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/ensureNotificationPermission | Non commencé | Non commencé |
| src/services/notifications.ts:64 | cancelAutomaticReminders | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/cancelAutomaticReminders | Non commencé | Non commencé |
| src/services/notifications.ts:95 | registerPushDevice | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/registerPushDevice | Non commencé | Non commencé |
| src/services/notifications.ts:101 | pushDiagnostic | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/pushDiagnostic | Non commencé | Non commencé |
| src/services/notifications.ts:111 | updatePushPresence | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/updatePushPresence | Non commencé | Non commencé |
| src/services/notifications.ts:118 | unregisterPushDevice | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/unregisterPushDevice | Non commencé | Non commencé |
| src/services/notifications.ts:128 | saveNotificationPreferences | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/saveNotificationPreferences | Non commencé | Non commencé |
| src/services/notifications.ts:135 | syncLearningReminder | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/syncLearningReminder | Non commencé | Non commencé |
| src/services/notifications.ts:145 | testLocalNotification | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/testLocalNotification | Non commencé | Non commencé |
| src/services/notifications.ts:150 | scheduledReminderCounts | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/scheduledReminderCounts | Non commencé | Non commencé |
| src/services/notifications.ts:159 | notificationDestination | rpc:register_push_device, from:push_devices, rpc:my_push_delivery_status, from:notification_preferences | data/notificationDestination | Non commencé | Non commencé |
| src/services/offlineSync.ts:6 | observeOfflineSync | Local / via services | data/observeOfflineSync | Non commencé | Non commencé |
| src/services/offlineSync.ts:7 | flushPendingSync | Local / via services | data/flushPendingSync | Non commencé | Non commencé |
| src/services/problemReports.ts:10 | problemTypes | from:app_problem_reports | data/problemTypes | Non commencé | Non commencé |
| src/services/problemReports.ts:18 | chooseProblemScreenshot | from:app_problem_reports | data/chooseProblemScreenshot | Non commencé | Non commencé |
| src/services/problemReports.ts:27 | flushProblemReports | from:app_problem_reports | data/flushProblemReports | Non commencé | Non commencé |
| src/services/problemReports.ts:37 | sendProblemReport | from:app_problem_reports | data/sendProblemReport | Non commencé | Non commencé |
| src/services/problemReports.ts:47 | observeProblemReportSync | from:app_problem_reports | data/observeProblemReportSync | Non commencé | Non commencé |
| src/services/problemReports.ts:48 | adminProblemReports | from:app_problem_reports | ui/AdminReportsPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/services/problemReports.ts:49 | resolveProblemReport | from:app_problem_reports | ui/AdminReportsPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/services/problemReports.ts:50 | problemScreenshotUrl | from:app_problem_reports | data/problemScreenshotUrl | Non commencé | Non commencé |
| src/services/quiz.ts:12 | cachedQuiz | Local / via services | data/cachedQuiz | Non commencé | Non commencé |
| src/services/quiz.ts:14 | quizRpc | Local / via services | data/quizRpc | Non commencé | Non commencé |
| src/services/quiz.ts:16 | refreshQuiz | Local / via services | data/refreshQuiz | Non commencé | Non commencé |
| src/services/quiz.ts:24 | answerDaily | Local / via services | data/answerDaily | Non commencé | Non commencé |
| src/services/quiz.ts:26 | createQuizChallenge | Local / via services | data/createQuizChallenge | Non commencé | Non commencé |
| src/services/quiz.ts:27 | answerChallenge | Local / via services | data/answerChallenge | Non commencé | Non commencé |
| src/services/quiz.ts:28 | useQuiz | Local / via services | data/useQuiz | Non commencé | Non commencé |
| src/services/quiz.ts:29 | observeQuizSync | Local / via services | data/observeQuizSync | Non commencé | Non commencé |
| src/services/quranAudioTimeline.ts:8 | chapterAudio | Local / via services | audio/chapterAudio | Non commencé | Non commencé |
| src/services/quranDownload.ts:17 | quranDownloaded | Local / via services | data/quranDownloaded | Non commencé | Non commencé |
| src/services/quranDownload.ts:18 | quranDownloadState | Local / via services | data/quranDownloadState | Non commencé | Non commencé |
| src/services/quranDownload.ts:19 | subscribeQuranDownload | Local / via services | data/subscribeQuranDownload | Non commencé | Non commencé |
| src/services/quranDownload.ts:20 | quranLineUri | Local / via services | data/quranLineUri | Non commencé | Non commencé |
| src/services/quranDownload.ts:21 | pauseQuranDownload | Local / via services | data/pauseQuranDownload | Non commencé | Non commencé |
| src/services/quranDownload.ts:33 | quranDownloadError | Local / via services | data/quranDownloadError | Non commencé | Non commencé |
| src/services/quranDownload.ts:40 | ensureQuranDownloaded | Local / via services | data/ensureQuranDownloaded | Non commencé | Non commencé |
| src/services/quranSourceReady.ts:9 | ensureQuranSourcePage | Local / via services | data/ensureQuranSourcePage | Non commencé | Non commencé |
| src/services/recitations.ts:20 | localRecitations | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/localRecitations | Non commencé | Non commencé |
| src/services/recitations.ts:24 | saveLocalRecitation | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/saveLocalRecitation | Non commencé | Non commencé |
| src/services/recitations.ts:35 | deleteLocalRecitation | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/deleteLocalRecitation | Non commencé | Non commencé |
| src/services/recitations.ts:41 | syncPendingRecitations | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/syncPendingRecitations | Non commencé | Non commencé |
| src/services/recitations.ts:68 | listRemoteRecitations | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | ui/RecitationsScreen + ui/AdminRecitationsPanel | En développement | Bloqué — serveur |
| src/services/recitations.ts:79 | markRecitationListened | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | ui/AdminRecitationsPanel | En développement | Bloqué — serveur |
| src/services/recitations.ts:85 | listCorrections | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | ui/RecitationsScreen + ui/AdminRecitationsPanel | En développement | Bloqué — serveur |
| src/services/recitations.ts:92 | listGeneralFeedback | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | ui/RecitationsScreen + ui/AdminRecitationsPanel | En développement | Bloqué — serveur |
| src/services/recitations.ts:99 | publishGeneralFeedback | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/publishGeneralFeedback | Non commencé | Non commencé |
| src/services/recitations.ts:108 | myCorrectionMarkers | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/myCorrectionMarkers | Non commencé | Non commencé |
| src/services/recitations.ts:120 | adminCorrectionIds | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | ui/AdminRecitationsPanel | En développement | Bloqué — serveur |
| src/services/recitations.ts:127 | signedAudioUrl | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/Repository.signedRecitation | En développement | Bloqué — serveur |
| src/services/recitations.ts:133 | deleteMyRecitation | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/deleteMyRecitation | Non commencé | Non commencé |
| src/services/recitations.ts:145 | publishCorrections | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | data/publishCorrections | Non commencé | Non commencé |
| src/services/recitations.ts:160 | finalizeRecitationCorrection | from:recitations, from:recitation_corrections, from:recitation_feedback, rpc:finalize_recitation_correction | ui/AdminRecitationsPanel + domain/recitationCorrectionPayload | En développement | Bloqué — serveur |
| src/services/social.ts:19 | ensureSocialProfile | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:20 | mySocialProfile | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/mySocialProfile | Non commencé | Non commencé |
| src/services/social.ts:24 | updateSocialProfile | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:28 | listFriendLinks | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:44 | cachedFriendsSnapshot | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/cachedFriendsSnapshot | Non commencé | Non commencé |
| src/services/social.ts:45 | cachedFriendInbox | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/cachedFriendInbox | Non commencé | Non commencé |
| src/services/social.ts:46 | clearFriendsSnapshot | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/clearFriendsSnapshot | Non commencé | Non commencé |
| src/services/social.ts:47 | loadFriendsSnapshot | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/loadFriendsSnapshot | Non commencé | Non commencé |
| src/services/social.ts:56 | loadFriendExtras | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/loadFriendExtras | Non commencé | Non commencé |
| src/services/social.ts:61 | prefetchFriendsSnapshot | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/prefetchFriendsSnapshot | Non commencé | Non commencé |
| src/services/social.ts:68 | loadFriendInbox | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/loadFriendInbox | Non commencé | Non commencé |
| src/services/social.ts:90 | sendFriendRequest | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:92 | openAdminContact | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:93 | acceptFriend | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:94 | declineFriend | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:95 | removeFriend | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:96 | blockFriend | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:97 | unblockFriend | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:98 | friendOverview | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:103 | publishSocialProgress | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/publishSocialProgress | Non commencé | Non commencé |
| src/services/social.ts:112 | setSocialOnline | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:114 | listGroups | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:115 | listGroupMembers | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:122 | createGroup | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:123 | inviteGroupMember | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:124 | acceptGroupInvite | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:125 | declineGroupInvite | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:126 | setGroupModerator | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:127 | removeGroupMember | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:128 | deleteGroup | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:130 | listMessages | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:144 | hideMessageForMe | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:148 | markConversationRead | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:152 | otherReadAt | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:156 | unreadMessageCount | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/unreadMessageCount | Non commencé | Non commencé |
| src/services/social.ts:157 | sendMessage | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:161 | shareRecitation | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/shareRecitation | Non commencé | Non commencé |
| src/services/social.ts:166 | conversationSummaries | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/conversationSummaries | Non commencé | Non commencé |
| src/services/social.ts:185 | deleteMessage | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:186 | reportMessage | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:187 | listGroupReports | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/listGroupReports | Non commencé | Non commencé |
| src/services/social.ts:193 | listSharedGoals | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:194 | proposeSharedGoal | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:198 | acceptSharedGoal | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:199 | listAppointments | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:200 | proposeAppointment | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:204 | acceptAppointment | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:205 | cancelAppointment | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/SocialService.kt + ui/SocialScreens.kt | Implémenté | Bloqué — serveur non connecté |
| src/services/social.ts:207 | isSocialAdmin | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/isSocialAdmin | Non commencé | Non commencé |
| src/services/social.ts:211 | mySocialSuspension | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/mySocialSuspension | Non commencé | Non commencé |
| src/services/social.ts:215 | listAdminReports | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/listAdminReports | Non commencé | Non commencé |
| src/services/social.ts:218 | listAdminMessages | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/listAdminMessages | Non commencé | Non commencé |
| src/services/social.ts:221 | adminProfiles | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/adminProfiles | Non commencé | Non commencé |
| src/services/social.ts:225 | listSocialSuspensions | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/listSocialSuspensions | Non commencé | Non commencé |
| src/services/social.ts:228 | resolveReport | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/resolveReport | Non commencé | Non commencé |
| src/services/social.ts:229 | suspendMember | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/suspendMember | Non commencé | Non commencé |
| src/services/social.ts:232 | unsuspendMember | from:friend_profiles, from:friend_links, rpc:friend_inbox, from:friend_groups, from:friend_group_members, from:friend_messages, from:friend_message_hidden, from:recitations, from:friend_message_reads, from:friend_message_reports, from:friend_shared_goals, from:friend_review_appointments, from:app_admins, from:social_suspensions | data/unsuspendMember | Non commencé | Non commencé |
| src/services/storage.ts:12 | loadState | Local / via services | data/loadState | Non commencé | Non commencé |
| src/services/storage.ts:20 | saveState | Local / via services | data/saveState | Non commencé | Non commencé |
| src/services/storage.ts:25 | loadAccountState | Local / via services | data/loadAccountState | Non commencé | Non commencé |
| src/services/storage.ts:33 | enqueueState | Local / via services | data/enqueueState | Non commencé | Non commencé |
| src/services/storage.ts:38 | pendingOperations | Local / via services | data/pendingOperations | Non commencé | Non commencé |
| src/services/storage.ts:43 | acknowledgeOperation | Local / via services | data/acknowledgeOperation | Non commencé | Non commencé |
| src/services/storage.ts:45 | saveMutation | Local / via services | data/saveMutation | Non commencé | Non commencé |
| src/services/sync.ts:8 | syncConfigured | from:user_state | data/syncConfigured | Non commencé | Non commencé |
| src/services/sync.ts:9 | supabase | from:user_state | data/supabase | Non commencé | Non commencé |
| src/services/sync.ts:15 | currentUser | from:user_state | data/currentUser | Non commencé | Non commencé |
| src/services/sync.ts:16 | signIn | from:user_state | data/signIn | Non commencé | Non commencé |
| src/services/sync.ts:22 | signOut | from:user_state | data/signOut | Non commencé | Non commencé |
| src/services/sync.ts:23 | requestPasswordLink | from:user_state | data/requestPasswordLink | Non commencé | Non commencé |
| src/services/sync.ts:28 | resendSignupConfirmation | from:user_state | Repository + MainActivity + AccountScreen | Implémenté | Testé (contrats JVM) ; serveur authentifié Bloqué |
| src/services/sync.ts:33 | consumeAuthLink | from:user_state | Repository + MainActivity + AccountScreen | Implémenté | Testé (contrats JVM) ; serveur authentifié Bloqué |
| src/services/sync.ts:44 | changePassword | from:user_state | data/changePassword | Non commencé | Non commencé |
| src/services/sync.ts:49 | pullState | from:user_state | data/pullState | Non commencé | Non commencé |
| src/services/sync.ts:57 | pushState | from:user_state | data/pushState | Non commencé | Non commencé |
| src/services/verseAudioCache.ts:5 | cachedVerseAudio | Local / via services | QuranAudioCache | Implémenté | Testé (Android, lecture après disparition de la source) |
| src/SocialScreens.tsx:36 | FriendsScreen | Local / via services | ui/FriendsScreen | Non commencé | Non commencé |
| src/SocialScreens.tsx:212 | AdminScreen | Local / via services | ui/AdminScreen | Non commencé | Non commencé |
| src/SurahPicker.tsx:8 | SurahPicker | Local / via services | ui/SurahPicker | Non commencé | Non commencé |
| src/theme/fonts.ts:5 | applyUiFont | Local / via services | ui/applyUiFont | Non commencé | Non commencé |
| src/theme/fonts.ts:6 | interfaceFont | Local / via services | ui/interfaceFont | Non commencé | Non commencé |
| src/theme/fonts.ts:7 | fontAssets | Local / via services | ui/fontAssets | Non commencé | Non commencé |
| src/theme/fonts.ts:12 | useUiFonts | Local / via services | ui/useUiFonts | Non commencé | Non commencé |
| src/theme/fonts.ts:13 | titleFont | Local / via services | ui/titleFont | Non commencé | Non commencé |
| src/theme/fonts.ts:14 | arabicFont | Local / via services | ui/arabicFont | Non commencé | Non commencé |
| src/theme/tokens.ts:1 | spacing | Local / via services | ui/spacing | Non commencé | Non commencé |
| src/theme/tokens.ts:2 | radius | Local / via services | ui/radius | Non commencé | Non commencé |
| src/theme/tokens.ts:3 | typography | Local / via services | ui/typography | Non commencé | Non commencé |
| src/theme/tokens.ts:4 | shadows | Local / via services | ui/shadows | Non commencé | Non commencé |
| src/theme/tokens.ts:5 | accents | Local / via services | ui/accents | Non commencé | Non commencé |
| src/ui/AdminProblemReports.tsx:6 | AdminProblemReports | Local / via services | ui/AdminReportsPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/ui/AdminQuiz.tsx:9 | AdminQuiz | Local / via services | ui/AdminQuizPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/ui/AdminQuizSets.tsx:8 | AdminQuizSets | Local / via services | ui/AdminQuizPanel | En développement | Bloqué — serveur ; tests contrats JVM |
| src/ui/AppearanceScreen.tsx:6 | AppearanceScreen | Local / via services | ui/AppearanceScreen | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:8 | readingArt | Local / via services | ui/readingArt | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:9 | Heading | Local / via services | ui/Heading | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:10 | ArabicLabel | Local / via services | ui/ArabicLabel | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:11 | IconButton | Local / via services | ui/IconButton | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:12 | SectionHeader | Local / via services | ui/SectionHeader | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:13 | SegmentedControl | Local / via services | ui/SegmentedControl | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:14 | QuranNumberMedallion | Local / via services | ui/QuranNumberMedallion | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:15 | DailyTaskCard | Local / via services | ui/DailyTaskCard | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:16 | StatCard | Local / via services | ui/StatCard | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:17 | ThemeSelector | Local / via services | ui/ThemeSelector | Non commencé | Non commencé |
| src/ui/DesignSystem.tsx:18 | AccentSelector | Local / via services | ui/AccentSelector | Non commencé | Non commencé |
| src/ui/FriendAvatar.tsx:6 | FriendAvatar | Local / via services | ui/FriendAvatar | Non commencé | Non commencé |
| src/ui/GoalScreen.tsx:9 | GoalScreen | Local / via services | ui/GoalScreen | Non commencé | Non commencé |
| src/ui/HomeIcon.tsx:4 | iconBackgrounds | Local / via services | ui/iconBackgrounds | Non commencé | Non commencé |
| src/ui/HomeIcon.tsx:5 | HomeIcon | Local / via services | ui/HomeIcon | Non commencé | Non commencé |
| src/ui/ImmersiveReaderChrome.tsx:6 | ReaderFloatingActions | Local / via services | ui/ReaderFloatingActions | Non commencé | Non commencé |
| src/ui/MainScreens.tsx:20 | Home | Local / via services | ui/Home | Non commencé | Non commencé |
| src/ui/MainScreens.tsx:28 | QuranScreen | Local / via services | ui/QuranScreen | Non commencé | Non commencé |
| src/ui/MainScreens.tsx:35 | ProgramScreen | Local / via services | ui/ProgramScreen | Non commencé | Non commencé |
| src/ui/MainScreens.tsx:46 | ProgressScreen | Local / via services | ui/ProgressScreen | Non commencé | Non commencé |
| src/ui/MessagingButton.tsx:7 | MessagingButton | Local / via services | ui/MessagingButton | Non commencé | Non commencé |
| src/ui/Premium.tsx:9 | themeArt | Local / via services | ui/themeArt | Non commencé | Non commencé |
| src/ui/Premium.tsx:11 | Icon | Local / via services | ui/Icon | Non commencé | Non commencé |
| src/ui/Premium.tsx:12 | premiumShadow | Local / via services | ui/premiumShadow | Non commencé | Non commencé |
| src/ui/Premium.tsx:13 | IslamicHero | Local / via services | ui/IslamicHero | Non commencé | Non commencé |
| src/ui/Premium.tsx:14 | ProgressTrack | Local / via services | ui/ProgressTrack | Non commencé | Non commencé |
| src/ui/Premium.tsx:15 | ProgressRing | Local / via services | ui/ProgressRing | Non commencé | Non commencé |
| src/ui/Premium.tsx:16 | GoldIcon | Local / via services | ui/GoldIcon | Non commencé | Non commencé |
| src/ui/Premium.tsx:17 | SessionToolbar | Local / via services | ui/SessionToolbar | Non commencé | Non commencé |
| src/ui/Premium.tsx:22 | mainTabs | Local / via services | ui/mainTabs | Non commencé | Non commencé |
| src/ui/Premium.tsx:23 | AppTopNavigation | Local / via services | ui/AppTopNavigation | Non commencé | Non commencé |
| src/ui/Premium.tsx:25 | BottomNavigation | Local / via services | ui/BottomNavigation | Non commencé | Non commencé |
| src/ui/ProblemReport.tsx:9 | ProblemReportCard | Local / via services | ui/ProblemReportCard | Non commencé | Non commencé |
| src/ui/ProblemReport.tsx:13 | ProblemReportSheet | Local / via services | ui/ProblemReportSheet | Non commencé | Non commencé |
| src/ui/ProfileHeaderButton.tsx:5 | ProfileHeaderButton | Local / via services | ui/ProfileHeaderButton | Non commencé | Non commencé |
| src/ui/QuizScreen.tsx:11 | QuizHomeCards | Local / via services | ui/QuizHomeCards | Non commencé | Non commencé |
| src/ui/QuizScreen.tsx:15 | QuizStats | Local / via services | ui/QuizStats | Non commencé | Non commencé |
| src/ui/QuizScreen.tsx:19 | QuizScreen | Local / via services | ui/QuizScreen | Non commencé | Non commencé |
| src/ui/QuranDownload.tsx:6 | DownloadSourceChoice | Local / via services | ui/DownloadSourceChoice | Non commencé | Non commencé |
| src/ui/QuranDownload.tsx:11 | QuranDownload | Local / via services | ui/QuranDownload | Non commencé | Non commencé |
| src/ui/QuranSessionHeader.tsx:7 | QuranSessionHeader | Local / via services | ui/QuranSessionHeader | Non commencé | Non commencé |
| src/ui/ReaderMoreSheet.tsx:5 | ReaderMoreSheet | Local / via services | ui/ReaderMoreSheet | Non commencé | Non commencé |
| src/ui/RevisionBottomActionBar.tsx:6 | RevisionBottomActionBar | Local / via services | ui/RevisionBottomActionBar | Non commencé | Non commencé |
| src/ui/StudySession.tsx:10 | StudyBanner | Local / via services | ui/StudyBanner | Non commencé | Non commencé |
| src/ui/StudySession.tsx:14 | SelectField | Local / via services | ui/SelectField | Non commencé | Non commencé |
| src/ui/StudySession.tsx:18 | StudyCompletionSheet | Local / via services | ui/StudyCompletionSheet | Non commencé | Non commencé |
| src/ui/StudySession.tsx:29 | StudyResumeCard | Local / via services | ui/StudyResumeCard | Non commencé | Non commencé |
| src/ui/theme.tsx:15 | colors | Local / via services | ui/colors | Non commencé | Non commencé |
| src/ui/theme.tsx:17 | applyTheme | Local / via services | ui/applyTheme | Non commencé | Non commencé |
| src/ui/theme.tsx:18 | useTheme | Local / via services | ui/useTheme | Non commencé | Non commencé |
| src/ui/theme.tsx:19 | themeOptions | Local / via services | ui/themeOptions | Non commencé | Non commencé |
| src/ui/theme.tsx:26 | Label | Local / via services | ui/Label | Non commencé | Non commencé |
| src/ui/theme.tsx:27 | Title | Local / via services | ui/Title | Non commencé | Non commencé |
| src/ui/theme.tsx:28 | Card | Local / via services | ui/Card | Non commencé | Non commencé |
| src/ui/theme.tsx:29 | Button | Local / via services | ui/Button | Non commencé | Non commencé |
| src/ui/theme.tsx:30 | Choice | Local / via services | ui/Choice | Non commencé | Non commencé |
| src/ui/theme.tsx:31 | CheckChoice | Local / via services | ui/CheckChoice | Non commencé | Non commencé |
| src/ui/theme.tsx:32 | Field | Local / via services | ui/Field | Non commencé | Non commencé |
| src/ui/ZoomableReader.tsx:5 | ZoomableReader | Local / via services | ui/ZoomableReader | Non commencé | Non commencé |

## Écrans et parcours

- src/AdminAccounts.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/AdminDailyContents.tsx : En développement ; UI Compose codée ; contrats JVM Testé, serveur Bloqué.
- src/AdminNotifications.tsx : En développement ; UI Compose codée ; contrats JVM Testé, serveur Bloqué.
- src/AdminRecitations.tsx : En développement ; UI Compose codée ; contrats JVM Testé, serveur Bloqué.
- src/AdminVoiceRecorder.tsx : En développement ; UI Compose codée ; contrats JVM Testé, serveur Bloqué.
- src/App.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/BookmarksScreen.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/coranTest/CoranTestScreen.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/coranTest/PageSurface.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/coranTest/PageSurface.web.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/DailyContentsScreen.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/MushafPage.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/PassageAudioPlayer.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/RecitationRecorder.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/RecitationsScreen.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ReviewDashboard.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/SocialScreens.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/SurahPicker.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/AdminProblemReports.tsx : En développement ; UI Compose codée ; contrats JVM Testé, serveur Bloqué.
- src/ui/AdminQuiz.tsx : En développement ; UI Compose codée ; contrats JVM Testé, serveur Bloqué.
- src/ui/AdminQuizSets.tsx : En développement ; UI Compose codée ; contrats JVM Testé, serveur Bloqué.
- src/ui/AppearanceScreen.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/DesignSystem.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/FriendAvatar.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/GoalScreen.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/HomeIcon.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/ImmersiveReaderChrome.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/MainScreens.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/MessagingButton.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/Premium.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/ProblemReport.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/ProfileHeaderButton.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/QuizScreen.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/QuranDownload.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/QuranSessionHeader.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/ReaderMoreSheet.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/RevisionBottomActionBar.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/StudySession.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/theme.tsx : Non commencé ; UI Compose ; tests Non commencé.
- src/ui/ZoomableReader.tsx : Non commencé ; UI Compose ; tests Non commencé.

## Limites connues

- La clé Supabase publique est maintenant injectée depuis la variable de compilation existante, sans secret committé ; authentification de comptes existants et RLS non testées.
- Le backend push utilise Expo. Ne jamais inscrire un jeton FCM dans la table Expo. Un canal FCM additif nécessite une proposition puis une autorisation avant toute modification serveur.
- Les polices Mushaf sont converties en TTF dans le projet Android, sans modifier les originaux. Les tailles décimales de chaque page sont conservées ; fidélité visuelle exhaustive à valider sur appareil.
- Les deux éditions en images conservent leurs pixels et rapports de dimensions.
- Le téléchargement Coran 1441 contient 9060 images de lignes : conserver les 15 lignes par page.
