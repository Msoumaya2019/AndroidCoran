# Avancement du portage Android

Estimation au 9 octobre 2026 : **environ 60 % de réalisation technique**, fourchette 55–65 %. Il s’agit d’une estimation de travail, pas d’une mesure de parité validée.

La matrice prioritaire compte 49 familles : 16 classées Implémenté, une Testé, 31 En développement et une Bloqué. Les familles partielles contiennent déjà du code natif, mais aussi des parcours ou validations manquants. Elles ont des tailles différentes : le nombre de lignes ou d’exports portés ne donne pas un pourcentage fiable.

Pour atteindre le livrable final, il reste notamment les comptes existants et la synchronisation authentifiée, les parcours sociaux et récitations sur serveur, les notifications push avec configuration FCM compatible, le téléchargement complet Coran 1441, la fidélité de tous les Mushafs et les interruptions système audio. Voir FUNCTIONAL_PARITY.md pour chaque limitation. Les tests publics Supabase ne valident pas les données privées d’un utilisateur existant.

La source Expo reste en lecture seule ; le développement et les commits sont exclusivement dans le dépôt public indépendant AndroidCoran. Aucun changement du backend partagé n’a été effectué.

Dernier lot publié : marquage manuel des difficultés, conservation des marqueurs administrateur, note hésitante et isolation de la navigation entre apprentissage et révision. Validation locale : 72 tests JVM et huit tests Android ciblés réussis ; la suite complète de 24 tests Android avait réussi au lot précédent. Ces ajouts réduisent les écarts mais ne changent pas significativement la fourchette globale de 55–65 %.

Lot suivant : validation précise des séances par verset, par page entièrement terminée ou par apprentissage entier ; choix de sourate et de note ; protection contre les points déjà validés et les bornes modifiées. Le parcours Android invité vérifie une validation partielle persistée puis sa reprise exacte. Validation : 79 tests JVM et 11 tests Android ciblés réussis. Les cartes de reprise ont été portées dans le lot suivant ; la synchronisation authentifiée reste à valider ; la fourchette globale reste une estimation.

Second lot de cette étape : cartes de reprise détaillées intégrées au programme, avec unités originales, état terminé/partiel/restant, expansion et reprise. Deux tests Compose supplémentaires et cinq tests Android de régression réussissent. La famille validation partielle/reprise est désormais Implémenté et testée localement ; son fonctionnement avec un compte réel reste à valider dans la famille synchronisation.

Actions de report du lecteur ajoutées : 81 tests JVM et six tests Android ciblés réussis. Les séances partielles conservent leur point de reprise et leurs validations. Lint et APK de débogage compilent.

Navigation directe du lecteur portée : les 114 sourates et un saut contrôlé vers les pages 1–604. Validation locale de ce second lot : 81 tests JVM et neuf tests Android ciblés, plus Lint et APK. Le test de navigation vérifie explicitement que changer de sourate efface le contexte de séance et que changer de page le conserve. Ces deux lots ne modifient pas significativement l’estimation globale de 55–65 %.

Consolidation : retour natif au tableau après sauvegarde et protection des soumissions. Marqueurs de difficulté : gestion compatible des champs inconnus/null. 83 tests JVM et trois tests Android ciblés réussis ; Lint et APK compilent.
