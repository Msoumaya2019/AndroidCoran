# AndroidCoran

Application Android native Kotlin / Jetpack Compose, indépendante de l'application Expo.

**Portage en cours : la parité complète n'est pas atteinte.** Consulter [FUNCTIONAL_PARITY.md](FUNCTIONAL_PARITY.md), [docs/VALIDATION.md](docs/VALIDATION.md) et les inventaires de référence avant toute utilisation en production.

## Compilation

JDK 17 ou plus récent, SDK Android 36 et Gradle Wrapper inclus :

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

L'APK debug est généré dans `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions publie cet APK après chaque push sur main. L'exécution des tests sur appareil nécessite un émulateur ou téléphone ; elle est distincte des tests JVM.

## Backend existant

Le projet reste `https://npbwnvrqmajwqtnncuyv.supabase.co`. Saisir sa **clé publique publishable/anon** dans Réglages. Aucun secret `service_role`, mot de passe ou clé privée n'est nécessaire à la compilation. Aucun SQL de migration n'est exécuté par cette application.

Les documents `user_state.data` conservent leurs champs JSON inconnus, pour éviter de supprimer des propriétés ajoutées par Expo ou Swift. Les sauvegardes locales sont séparées par compte. Un changement distant concurrent suspend les écritures plutôt que d'écraser les données ; la résolution et la fusion complète d'Expo restent à porter.

La connexion réutilise les comptes Auth existants. La clé réelle est absente de la référence : la connexion et les politiques RLS doivent être validées avec un compte de test avant toute diffusion.

## Ressources coraniques

Les données Hafs, métadonnées, traduction française, pages, coordonnées et glyphes sont copiés de la référence immuable. Les images sont conservées sans retouche. Les polices WOFF2 ont été converties en TTF pour Canvas Android ; la fidélité aux polices COLR et aux repères doit encore être comparée sur appareils. Conserver les licences et attributions ; `app/src/main/assets/TANZIL-LICENSE.txt` est inclus.

Toutes les pages de deux éditions sont embarquées pour l'accès hors connexion. L'APK est volumineux : préparer Play Asset Delivery ou des téléchargements vérifiés avant publication Google Play.

## Production et signature

`./gradlew assembleRelease bundleRelease` prépare les livrables non signés. Ajouter ultérieurement une configuration de signature injectée depuis un stockage sécurisé ou les secrets de CI ; ne jamais versionner de keystore ni mots de passe. La configuration Firebase/FCM et le routage push partagé nécessitent une adaptation additive autorisée (voir `docs/FCM_PROPOSAL.md`).

## Sécurité des dépôts

L'historique est créé de zéro. L'unique remote de push doit être `https://github.com/Msoumaya2019/AndroidCoran.git`. Le dépôt Expo est une source de lecture ; sa copie d'audit reste hors du dépôt Android. Aucun dépôt Swift n'est utilisé pour le développement.
