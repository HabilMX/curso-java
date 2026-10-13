# Leçon 1 — Installer Java 25 sur ton Linux Mint

**Durée :** 90 min (juste : l'installation dépend de ta connexion, et si quelque chose y échoue, le reste de la leçon est amputé ; ne te presse pas)

**Ce que tu construis :** l'environnement de travail (un JDK 25 qui fonctionne) et ton premier programme ; tu auras créé le dossier `~/curso-java/` avec le fichier `Hola.java` et le premier fichier du projet, `revisor/Revisor.java`

**Ce que tu apprends :** installer SDKMAN et, avec lui, Temurin 25, et vérifier la version dans un terminal neuf avec `java --version` et `javac --version` ; pourquoi on n'utilise pas `default-jdk` ; le fichier compact de Java 25 (`void main()` et `IO.println`) ; `java Hola.java` ; compiler avec `javac` et exécuter avec `java` séparément, et ce qu'est le `.class` ; essayer des idées avec `jshell` ; et lire la première erreur du compilateur

**D'où tu viens.** De la [Leçon 0](00-que-es-java.md), où tu as vu le chemin du code : source, `javac`, *bytecode* et la JVM ; la différence entre JDK et JRE ; et ce qu'est LTS. Cette leçon prend cette théorie et la transforme en commandes. Tu travailles sur un Linux Mint 22 (n'importe quelle 22.x : la 22.3, « Zena », est la version en vigueur d'après la [page des versions de Linux Mint](https://linuxmint.com/download_all.php), avec un support jusqu'en avril 2029), qui est basé sur Ubuntu « Noble ».

**Ce que cette leçon ne fait pas.** Elle n'enseigne pas la syntaxe de Java au-delà de ce qu'il faut pour le premier programme : types, chaînes et contrôle de flux relèvent de la [Leçon 2](02-tipos-cadenas-control.md). Elle n'installe pas Maven (la [Leçon 6](06-proyecto-maven.md) le fait quand il le faut). Et elle n'utilise pas d'environnement de développement graphique : tout se fait avec un éditeur de texte et un terminal, parce que c'est ainsi que l'on voit ce que fait chaque pièce, et parce que c'est ce que tu auras sur un serveur.

**À propos des exécutions et de ce que nous n'avons pas pu vérifier.** Les programmes de cette leçon et leurs messages d'erreur ont été exécutés avec Temurin 25.0.4 sous macOS et comparés caractère par caractère avec ce qui est montré ici ; ceux qui montrent Java 21 et Java 17 ont été obtenus avec Temurin 21.0.12 et 17.0.20. Sur ton Linux Mint, ils devraient donner la même chose, parce que ce sont des messages du compilateur et de la JVM et non du système, mais ces programmes n'ont pas été exécutés sous Mint. **Ce qui dépend bel et bien de ton Linux Mint**, c'est l'installation, et celle-ci a été mesurée le 13 octobre 2026 sur un vrai Linux Mint 22.3 : ce que répond le terminal quand il n'y a pas de Java, ce qu'imprime l'installateur de SDKMAN, quels identifiants de Temurin il proposait ce jour-là, ce que dit ensuite `java --version`, et le piège du terminal qui ne se rend pas compte du changement. Si quelque chose de ce que tu vois à l'écran diffère de ce qui est montré ici sur ces points, note-le dans ton [journal de bord](bitacora.md) : c'est une information utile. Les programmes se trouvent dans [`programas/01-instalacion/`](../programas/01-instalacion), dans le dépôt du cours.

## À la fin, tu seras capable de

- Installer SDKMAN et, avec lui, Temurin 25 en choisissant le bon identifiant dans `sdk list java`, et vérifier dans un terminal neuf, avec `java --version` et `javac --version`, que ce sont ceux de la version 25.
- Expliquer pourquoi `sudo apt install default-jdk` te donne Java 21 sur Mint 22, et quelle erreur cela produit avec le fichier compact.
- Écrire, exécuter et modifier un programme Java d'un seul fichier avec `void main()` et `IO.println`, et l'exécuter avec `java Hola.java`.
- Compiler avec `javac`, exécuter avec `java`, et dire ce qu'est le fichier `.class` qui est apparu et quand il faut recompiler.
- Essayer des expressions isolées dans `jshell` et en sortir.
- Lire une erreur du compilateur : le fichier, la ligne, la flèche `^`, et les lignes `symbol` et `location`.
- Créer le premier fichier du projet, `~/curso-java/revisor/Revisor.java`, et l'exécuter.

## Le pourquoi avant le comment

Il y a une raison pour laquelle cette leçon arrive si tôt dans le cours : **un environnement qui ne fonctionne pas rend impossible tout ce qui suit**, et beaucoup des problèmes que les gens ont avec Java dans leurs premiers jours sont des problèmes d'installation et de version, pas de programmation : on a installé une version différente de celle que suppose le tutoriel, ou l'on en a plus d'une d'installée et l'on utilise la mauvaise. C'est pourquoi la première étape de la leçon n'est pas d'écrire du code, mais de **découvrir ce que tu as**, et la dernière de la partie installation est de **vérifier quelle version tu utilises**. Tout le reste, le fichier `Hola.java`, `javac`, `jshell`, se construit sur la certitude que la commande `java` de ton terminal est celle de Java 25.

Il y a aussi une raison de moral : qui arrive à ce cours programme déjà, et ce dont il a besoin d'abord, c'est d'un succès rapide. Le programme que tu écriras dans la section 1.2 fait trois lignes, et tu l'exécutes avec une seule commande. Avant Java 25, ce même programme demandait de déclarer une classe et une méthode `public static void main(String[] args)` autour de lui ; depuis Java 25, c'est ce que tu vois ci-dessous. C'est la meilleure nouvelle de Java pour qui vient d'un autre langage, et c'est pourquoi le cours en profite.

La leçon comporte sept parties : **installer le JDK** (1.1), **ton premier programme** (1.2), **compiler et exécuter séparément** (1.3), **`jshell`** (1.4), **lire la première erreur** (1.5), **quand la version est la mauvaise** (1.6) et **le style du code** (1.7), et elle se termine par la première étape du **projet** (1.8). À la fin de la leçon, voici ce que tu verras en exécutant le `revisor`, qui pour l'instant ne vérifie rien, mais qui existe déjà (le programme imprime « revisor: todavía no revisa nada », c'est-à-dire « revisor : il ne vérifie encore rien ») :

```text
revisor: todavía no revisa nada
```

## Les concepts

### 1.1 Installer le JDK

#### 1.1.1 D'abord : ce que tu as

Ouvre un terminal (sous Mint : `Ctrl`+`Alt`+`T`) et pose deux questions. La première, quel système c'est :

```bash
cat /etc/os-release
```

Parmi les lignes qu'il imprime, cherche `VERSION_CODENAME` et `UBUNTU_CODENAME`. Sous Linux Mint, le premier est le nom de Mint (dans la 22.3, `zena`) et le second est celui de la version d'Ubuntu sur laquelle il est basé (`noble`). Pour installer Java avec la méthode de ce cours, tu n'auras besoin d'aucun des deux, mais il vaut mieux le savoir : beaucoup de recettes sur internet ont été écrites pour Ubuntu et trouvent « le nom de la version » avec la commande `lsb_release -cs`, qui sur Mint 22.3 répond `zena`, pas `noble` (mesuré sur une Mint 22.3). Une recette qui utilise cette réponse pour construire l'adresse d'un dépôt te fait pointer vers une adresse qui n'existe pas. Si un jour une recette d'Ubuntu échoue sur ta Mint, commence par là.

La seconde question, s'il y a déjà Java :

```bash
java --version
```

Trois choses peuvent se produire. S'il n'y a pas de Java, le terminal répond qu'il ne trouve pas la commande. Sur une Mint 22.3 fraîchement installée, on a mesuré `java: not found`, et quelque chose de plus utile que le texte : le **code de sortie**, le nombre que tout programme laisse en terminant et que tu vois en tapant `echo $?` juste après. Il valait **127**, le nombre que le terminal réserve pour « cette commande n'existe pas ». Le texte de l'avertissement peut varier d'un terminal à l'autre ; le 127, non. (Si tu préfères poser la question sans rien exécuter, `which java` cherche le programme dans ton système : sans Java, il n'imprime rien et termine avec le code 1. Ce sont deux codes différents pour deux questions différentes : « exécute ceci » et « où est ceci ? ».) S'il imprime une version qui commence par 21 (ou 11, ou 17), quelqu'un a installé Java avant, peut-être sans s'en rendre compte, parce que beaucoup de programmes l'apportent comme dépendance ; cela ne gêne pas, et en 1.1.7 tu verras pourquoi il n'est pas nécessaire de l'enlever. Et s'il imprime 25 et dit `Temurin`, tu as déjà ce que le cours utilise et tu peux sauter à 1.1.6 pour confirmer que tu as aussi le compilateur.

#### 1.1.2 Pourquoi pas `default-jdk`, et pourquoi SDKMAN

Si tu demandes à un moteur de recherche comment installer Java sur Ubuntu ou sur Mint, la réponse la plus fréquente est celle-ci :

```bash
sudo apt install default-jdk
```

**Ne le fais pas pour ce cours.** Le paquet `default-jdk` n'est pas « la version de Java la plus récente » : c'est « la version de Java que la distribution a choisie par défaut », et ce choix est figé quand la distribution sort. Sur Ubuntu Noble, la base de Mint 22, le paquet `default-jdk` dépend de `openjdk-21-jdk` (selon sa [page dans l'archive de paquets d'Ubuntu](https://packages.ubuntu.com/noble/default-jdk), consultée le 7 octobre 2026, et mesuré sur une Mint 22.3 : `apt` propose `default-jdk` dans sa version `2:1.21-75`, celle de Java 21). Autrement dit, il installe **Java 21**. Java 21 est une version LTS respectable, mais pas celle qu'enseigne ce cours, et ce qui compte pour la leçon, c'est ce qu'elle ne peut pas faire : **elle n'exécute pas le fichier compact avec `void main()`**. Tu le verras en 1.6 avec son message d'erreur exact. Qui installe `default-jdk` et suit cette leçon voit échouer le premier programme et, sans savoir pourquoi, conclut que le cours est mauvais. Il ne l'est pas ; c'est la version qui est la mauvaise.

Le cours utilise **Eclipse Temurin 25**, la distribution avec laquelle toutes ses sorties ont été mesurées (la Leçon 0 a expliqué ce qu'est une distribution), et l'installe avec **SDKMAN**, un gestionnaire de versions pour les outils Java. Trois raisons en décident. La première : SDKMAN s'installe dans ton dossier personnel (`~/.sdkman/`), **sans `sudo`**, sans toucher aux programmes du système ; si quelque chose tourne mal, tu ne casses rien de ce que Mint utilise. La deuxième : il permet d'avoir plusieurs versions de Java à la fois et de choisir laquelle utiliser avec une commande, ce dont tu auras besoin le jour où un projet extérieur exigera une autre version. La troisième : avec le même outil, tu installeras Maven dans la [Leçon 6](06-proyecto-maven.md), avec la version exacte du cours. Une seule méthode pour tout, la même sur chaque machine.

#### 1.1.3 Ce dont SDKMAN a besoin

SDKMAN s'installe avec un script `bash` qui télécharge et décompresse des fichiers, et pour cela il utilise quelques outils du système. En s'exécutant, l'installateur cherche `bash`, `unzip`, `zip`, `tar`, `find`, `curl` et `sed` (mesuré le 13 octobre 2026 sur un Linux Mint 22.3). `bash`, `tar`, `find` et `sed` sont toujours livrés avec Mint. Les trois qui peuvent manquer sont `curl` (le programme qui télécharge des choses depuis le terminal), `zip` et `unzip`. Pour le vérifier :

```bash
which curl zip unzip
```

`which` imprime le chemin de chaque programme qu'il trouve et se tait pour celui qu'il ne trouve pas. Si tu vois les trois chemins, continue. S'il en manque un, installe-le avec le gestionnaire de paquets de Mint (c'est la seule fois où le cours utilise `sudo`, parce que ceux-là sont bien des outils du système) :

```bash
sudo apt install curl zip unzip
```

`sudo` exécute ce qui suit avec des droits d'administrateur et te demande ton mot de passe (en le tapant, tu ne verras pas d'astérisques ; c'est normal). `apt install` installe les paquets que tu nommes ; si l'un d'eux était déjà là, il le dit et continue avec les autres.

#### 1.1.4 Installer SDKMAN

L'installateur est un script que le projet lui-même publie sur son site officiel. La [page d'installation de SDKMAN](https://sdkman.io/install/) donne cette commande :

```bash
curl -s "https://get.sdkman.io" | bash
```

Lis-la par morceaux. `curl -s "https://get.sdkman.io"` télécharge le script d'installation depuis le site de SDKMAN, par `https` (la connexion chiffrée qui garantit que tu parles à ce site et que personne n'a changé le contenu en chemin), et `-s` lui retire la barre de progression. Le tube `|` passe ce texte à `bash`, qui l'exécute. Autrement dit : tu télécharges un programme et tu l'exécutes sur la même ligne. Cette forme a mauvaise réputation, et il vaut mieux savoir exactement pourquoi. Ce qui est dangereux n'est pas la forme, mais **d'où vient** ce que tu exécutes : un script d'un site que tu ne connais pas, ou collé depuis un forum, peut faire n'importe quoi avec ton utilisateur. Ici, l'origine est le site officiel du projet, par `https`, et le script ne demande pas `sudo` : il travaille seulement dans ton dossier personnel. Et si tu veux le voir avant de l'exécuter, tu peux le faire en deux étapes, ce qui est une bonne habitude avec n'importe quel installateur : `curl -s "https://get.sdkman.io" -o instalar-sdkman.sh` l'enregistre dans un fichier, tu le lis avec `less instalar-sdkman.sh`, et tu l'exécutes avec `bash instalar-sdkman.sh`.

Voici ce qu'a imprimé l'installateur le 13 octobre 2026 sur un Linux Mint 22.3 (abrégé aux lignes qui comptent : les trois points marquent ce qui a été retiré, et le chemin de ton dossier personnel s'affiche sous la forme `~`) :

```text
$ curl -s "https://get.sdkman.io" | bash
...
Looking for a previous installation of SDKMAN...
Checking Bash version...
Looking for unzip...
Looking for zip...
Looking for tar...
Looking for find...
Looking for curl...
Looking for sed...
Installing SDKMAN scripts...
...
Set version to 5.23.2 ...
...
Added sdkman init snippet to ~/.bashrc
...
All done!
...
Please open a new terminal, or run the following in the existing one:

    source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Cela se lit de haut en bas : il cherche une installation précédente, vérifie que tu as une version de `bash` qui lui convient, cherche un à un les outils de 1.1.3, installe ses scripts dans `~/.sdkman/` et ajoute à ton fichier `~/.bashrc` un fragment qui charge SDKMAN chaque fois qu'un terminal s'ouvre. La ligne finale est celle qu'il faut suivre : **le terminal dans lequel tu es ne sait pas encore que SDKMAN existe**, parce qu'il a lu `~/.bashrc` à son ouverture, avant l'installation. Soit tu ouvres un terminal neuf, soit tu demandes à celui-ci de charger SDKMAN tout de suite, avec la commande `source` qu'il te montre. Ensuite, vérifie :

```text
$ sdk version

SDKMAN!
script: 5.23.2
native: 0.7.34 (linux x86_64)
```

Si, à la place, tu vois `sdk: command not found`, c'est que le terminal n'a pas chargé SDKMAN. Ouvre-en un nouveau, ou charge SDKMAN à la main dans celui-ci avec `source "$HOME/.sdkman/bin/sdkman-init.sh"` et reteste `sdk version`. Si `source` dit que ce fichier n'existe pas, l'installation ne s'est pas terminée (presque toujours à cause d'un outil de 1.1.3 qui manquait ou d'une coupure de connexion) : lis le message de l'installateur, corrige la cause et relance la commande d'installation. Les numéros de version seront ceux de ton jour ; ce qui compte, c'est que `sdk` réponde.

#### 1.1.5 Choisir et installer Temurin 25

SDKMAN ne sait pas quel Java tu veux : tu le choisis par son **identifiant**. Pour voir ceux qu'il propose :

```bash
sdk list java
```

Il imprime un long tableau, avec une section par distribution (Corretto, GraalVM, Liberica, Temurin, Zulu et d'autres). Cherche celle de **Temurin** : ses identifiants se terminent par **`-tem`**. Le 13 octobre 2026, sur une Mint 22.3, cette section contenait ceux-ci : `27.0.0.0+35-tem`, `26.0.2+1.1-tem`, `25.0.3.0+9-tem`, `21.0.12+1.1-tem`, `17.0.20-tem`, `11.0.32+1.1-tem` et `8.0.504+1-tem`. La règle pour choisir est unique : **celui qui commence par `25` et se termine par `-tem`**. Ce jour-là, c'était `25.0.3.0+9-tem` ; sur ton écran, ce peut être un autre, parce que le numéro complet change à chaque mise à jour de Temurin. Ne copie pas celui de cette leçon : copie celui de ta liste.

Un détail mesuré qu'il vaut mieux connaître pour ne pas se tromper : au bas de la liste, SDKMAN suggère un identifiant par défaut (ce jour-là, il disait `install the default: 25.0.4-tem`), et cet identifiant **n'apparaissait pas dans la liste**. C'est pourquoi le cours ne te demande pas d'utiliser la suggestion, mais de choisir dans la liste ce qui est bel et bien disponible. Avec l'identifiant en main :

```text
$ sdk install java 25.0.3.0+9-tem
...
Downloading: java 25.0.3.0+9-tem
...
Repackaging Java 25.0.3.0+9-tem...
...
Installing: java 25.0.3.0+9-tem
Done installing!

Setting java 25.0.3.0+9-tem as default.
```

Il télécharge le JDK, le range et l'installe dans `~/.sdkman/candidates/java/`, et la dernière ligne dit quelque chose d'important : il le laisse comme **par défaut**, celui qui répondra à la commande `java` dans les terminaux que tu ouvriras à partir de maintenant. Selon le cas (par exemple, si tu avais déjà un autre Java installé avec SDKMAN), au lieu de le fixer il peut seulement te **demander** si tu veux qu'il soit celui par défaut : réponds oui, avec `Enter` ou `Y`. Si tu as répondu non, ou si tu n'es pas sûr, tu peux le rattraper à tout moment avec `sdk default java` et l'identifiant que tu as installé (ici, `sdk default java 25.0.3.0+9-tem`). Le téléchargement prend d'une à plusieurs minutes, selon ta connexion.

#### 1.1.6 Vérifier la version, dans un terminal neuf

Voici le piège qui fait perdre le plus de temps, et il a été mesuré sur la même Mint : **dans le terminal où tu as installé, `java --version` a continué de donner le Java précédent** (sur cette machine, il y en avait un du système, dans `/usr/bin/java`). Ce n'est pas que l'installation ait échoué : ce terminal a décidé où chercher les programmes à son ouverture, et il ne se rend pas compte du changement. **Ferme le terminal et ouvres-en un nouveau.** Là :

```bash
java --version
javac --version
```

Les deux commandes, et pas seulement la première, parce que `java` est le programme qui exécute et `javac` celui qui compile, et qu'une installation à moitié faite peut donner l'un sans l'autre (c'est exactement la différence entre un JRE et un JDK de la Leçon 0). Voici ce qu'a imprimé `java --version` le 13 octobre 2026 sur la Mint 22.3, dans le terminal neuf :

```text
$ java --version
openjdk 25.0.3 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-25.0.3+9 (build 25.0.3+9-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.3+9 (build 25.0.3+9-LTS, mixed mode, sharing)
```

Il y a deux choses que tu dois reconnaître dans la tienne : **la première ligne commence par `openjdk 25`** et **la deuxième dit `Temurin`**. Le reste change avec la mise à jour : `25.0.3` est la version (25 est la majeure ; `0.3` est le numéro de correctif), `2026-04-21` est la date de cette mise à jour, et `LTS` confirme que c'est une version à support long. Les sorties des autres leçons ont été mesurées sur une autre machine avec un correctif postérieur (Temurin 25.0.4) ; le correctif ne change rien de ce qu'enseigne le cours. `javac --version` doit dire `javac 25` suivi de son correctif. Et si tu veux savoir d'où vient le `java` qui a répondu, `which java` le dit : sur la Mint mesurée, le chemin à l'intérieur du dossier de SDKMAN qui se termine par `.sdkman/candidates/java/current/bin/java`. (`which` imprime le chemin complet, avec ton dossier personnel : `/home/<ton utilisateur>/.sdkman/candidates/java/current/bin/java` ; ici on le raccourcit avec `~`.) Si `which java` te donne `/usr/bin/java`, tu es dans un vieux terminal.

#### 1.1.7 Si tu as plus d'un Java

> **Plusieurs JDK à la fois.** Si tu avais déjà un Java du système, ou si un jour tu installes une autre version avec SDKMAN, inutile de désinstaller quoi que ce soit : SDKMAN choisit lequel répond à la commande `java`. `sdk list java` marque dans son tableau ceux que tu as installés. `sdk use java <identifiant>` change de version **uniquement dans le terminal actuel**, pour essayer quelque chose ; `sdk default java <identifiant>` la laisse comme version par défaut **dans tous les nouveaux terminaux**. Si `java --version` ne dit pas 25 et Temurin, la première chose est d'ouvrir un terminal neuf ; la deuxième, `sdk default java` avec l'identifiant de 1.1.5. Ensuite, répète la vérification de 1.1.6, qui se répète chaque fois que tu changes quelque chose.

### 1.2 Ton premier programme

#### 1.2.1 Le dossier et le fichier

Crée le dossier du cours et entres-y :

```bash
mkdir -p ~/curso-java/revisor
cd ~/curso-java
```

`mkdir -p` crée le dossier, ainsi que les dossiers intermédiaires s'ils manquent ; sans `-p`, il échouerait si `~/curso-java` n'existait pas quand on demande `revisor` à l'intérieur. `~` est un raccourci pour ton dossier personnel. Dans `~/curso-java/` vivra tout le cours, et à l'intérieur, `revisor/` gardera le projet qui grandit leçon après leçon.

Crée le fichier `Hola.java` avec l'éditeur de ton choix (Mint est livré avec Xed, qui s'ouvre avec `xed Hola.java`) et écris ceci. Respecte la majuscule initiale du nom et l'extension. La première ligne, `// fig01_01.java`, est un commentaire : ce qui suit `//` jusqu'à la fin de la ligne est ignoré par le compilateur. Dans le cours, ce commentaire est la marque qui dit comment le programme s'appelle dans le dépôt ; dans ton `Hola.java`, tu peux le copier ou non, cela revient au même :

```java
// fig01_01.java
void main() {
  IO.println("Hola desde Java");
}
```

Enregistre-le et, depuis le terminal, dans le dossier `~/curso-java`, exécute-le avec le nom de **ton** fichier :

```text
$ java Hola.java
Hola desde Java
```

(Le programme imprime « Hola desde Java », c'est-à-dire « Salut depuis Java ». Dans le dépôt du cours, le même programme s'appelle `fig01_01.java`, et `java fig01_01.java` imprime la même chose.)

C'est tout le programme. Voyons ce que dit chaque partie.

#### 1.2.2 Ligne par ligne

**`void main() {`** déclare le point d'entrée : la première chose que Java exécute quand le programme s'exécute. Il y a trois pièces. `main` est le nom que Java cherche pour commencer (ce n'est pas un mot réservé, c'est une convention du langage que le lanceur connaît). `()` sont les parenthèses où iraient les données que la méthode reçoit ; ici, elle n'en reçoit aucune. `void` veut dire « ne renvoie rien » : `main` fait des choses, mais ne rend pas de résultat à qui l'a appelée. Les accolades `{ }` délimitent le corps : tout ce qui est entre celle d'ouverture et celle de fermeture est ce que `main` fait. Cette « méthode » sans classe autour est une nouveauté de Java 25 : on l'appelle **fichier compact** ([JEP 512](https://openjdk.org/jeps/512)).

**`IO.println("Hola desde Java");`** imprime une ligne de texte. `IO` est une classe de la bibliothèque standard, `java.lang.IO`, ajoutée dans Java 25 pour l'entrée et la sortie simples ([documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/IO.html)) ; comme elle se trouve dans le paquet `java.lang`, que Java importe toujours, **il n'y a rien à importer**. `println` (« imprime une ligne ») écrit ce qu'on lui passe et un saut de ligne. Ce qui est entre les parenthèses, `"Hola desde Java"`, est un texte, qui en Java s'écrit entre guillemets doubles. Le point-virgule `;` termine l'instruction : en Java, il est obligatoire (à la différence de Python ou de Go), et l'oublier est l'erreur la plus courante du premier jour.

**`}`** ferme le corps.

Deux choses qu'il n'y a **pas** : une classe autour, et ni les mots `public static` ni les `String[] args`. Dans le reste du cours, c'est la norme. Qui a donc créé la classe ? Le compilateur, en silence : il prend ton fichier, donne à la classe le nom du fichier (`Hola`) et enveloppe ton `main` dedans. Tu le vérifieras en 1.3 avec `javap`.

#### 1.2.3 Comment on l'écrivait avant

Il est indispensable que tu reconnaisses l'ancienne forme, parce que c'est celle que tu verras dans presque tous les tutoriels, livres et réponses d'internet qui ne datent pas des deux dernières années. Le même programme, écrit comme on l'exigeait jusqu'à Java 24, est :

```java
public class Viejo {
  public static void main(String[] args) {
    System.out.println("Hola desde Java");
  }
}
```

Il s'exécute exactement pareil dans Java 25 (vérifié : il imprime `Hola desde Java`). Chaque mot supplémentaire avait une fonction, que la Leçon 6 explique en détail, et qu'ici on esquisse seulement pour qu'elle ne t'effraie pas quand tu la verras. `class Viejo { ... }` définit une classe, l'unité dans laquelle Java organise le code ; `public` la rend visible depuis l'extérieur du fichier. `static` signifie que la méthode appartient à la classe et non à un objet particulier, de sorte qu'on peut l'appeler sans rien créer avant. `String[] args` est la liste des arguments que l'utilisateur écrit après le nom du programme dans la ligne de commande. Et `System.out.println` est le long chemin pour imprimer : `System` est une classe, `out` est un objet à l'intérieur (la sortie standard) et `println` est ce qu'on lui demande.

Pour un programme d'une ligne, tout cela est de la cérémonie. Java 25 la retire **quand elle n'est pas nécessaire**, et elle redevient nécessaire quand le programme grandit. Le cours utilise la forme courte jusqu'à ce qu'il grandisse, et `IO.println` au lieu de `System.out.println`, qui dans ce cours est plus court à écrire et plus facile à lire. Si tu lis du code qui utilise `System.out.println`, il fait la même chose ; il n'y a pas à le changer.

#### 1.2.4 Le nom du fichier et d'autres détails qui mordent

Il y a quatre détails qu'il vaut mieux connaître dès maintenant. **Un :** dans un fichier compact, le compilateur utilise le nom du fichier comme nom de la classe, donc ce doit être un nom valide pour Java et, par convention, commencer par une majuscule (`Hola.java`, `Revisor.java`). **Deux :** le programme doit avoir une méthode `main` ; avec un autre nom, le compilateur le refuse avec un message qui le dit (tu le verras en 1.5). **Trois :** le texte du fichier doit être en UTF-8, ce que produisent par défaut les éditeurs de Mint. **Quatre :** `java Hola.java` et `java hola.java` sont des fichiers différents sous Linux, où les majuscules comptent ; sous macOS et Windows cela revient parfois au même, et c'est un classique du « chez moi ça marchait ».

Et un détail de plus sur ce que fait `java Hola.java` : le programme `java` voit que tu lui as donné un fichier `.java`, le **compile en mémoire** et l'exécute, sans rien laisser sur le disque. On l'appelle le mode de lancement d'un fichier source ([JEP 330](https://openjdk.org/jeps/330)) et il existe depuis Java 11. C'est celui que le cours utilise de la Leçon 1 à la 5.

### 1.3 `javac` et `java` séparément

#### 1.3.1 Compiler

Ce que `java Hola.java` fait en une seule étape peut se faire en deux, et le voir ainsi est la meilleure façon de comprendre ce qu'est un `.class`. Dans le dossier où se trouve `Hola.java` :

```bash
javac Hola.java
ls Hola.*
```

```text
$ ls Hola.*
Hola.class  Hola.java
```

`javac Hola.java` a compilé le fichier et, s'il n'y a pas eu d'erreurs, n'a rien imprimé (un compilateur qui ne dit rien est un compilateur content), mais il a laissé un nouveau fichier : `Hola.class`. Ce fichier est le *bytecode* de la Leçon 0. Il pèse 320 octets et commence par les huit octets que tu connais déjà (si ta Mint n'a pas `xxd`, saute cette commande : elle ne resservira pas) :

```text
$ xxd -l 8 Hola.class
00000000: cafe babe 0000 0045                      .......E
```

Pour voir ce qu'il contient sans le lire octet par octet, le JDK fournit le désassembleur `javap` :

```text
$ javap Hola
Compiled from "Hola.java"
final class Hola {
  Hola();
  void main();
}
```

Voilà la preuve de ce qui a été dit en 1.2.2 : le compilateur a créé une classe appelée `Hola` (comme le fichier), et a mis dedans ta méthode `main`, ainsi qu'un constructeur `Hola()` que tu n'as pas écrit (un constructeur est ce qui crée un objet de la classe ; la Leçon 3 l'explique). Avec `javap -c -p Hola`, tu verrais en plus les instructions de `main`, parmi lesquelles un appel à la méthode `println` de la classe `IO`.

#### 1.3.2 Exécuter

Pour exécuter le `.class`, on ne donne plus le fichier au lanceur mais **le nom de la classe** :

```text
$ java Hola
Hola desde Java
```

Sans extension : `Hola`, pas `Hola.class` ni `Hola.java`. C'est la confusion la plus fréquente de cette section. Les trois erreurs qu'elle produit ont été mesurées, et il vaut mieux les avoir sous les yeux ; la première est celle d'exécuter la classe **avant** de compiler, la deuxième celle d'y mettre l'extension, et la troisième celle d'oublier l'extension dans le compilateur :

```text
$ java Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola

$ java Hola.class
Error: Could not find or load main class Hola.class
Caused by: java.lang.ClassNotFoundException: Hola.class

$ javac Hola
error: Class names, 'Hola', are only accepted if annotation processing is explicitly requested
1 error
```

Les deux premiers ont été obtenus avec la langue de Java en anglais ; sur un système dont la configuration régionale est en espagnol, le même message sort traduit (par exemple, « Error: no se ha encontrado o cargado la clase principal Hola », c'est-à-dire « Erreur : classe principale Hola introuvable ou non chargée »). Apprends à les reconnaître dans les deux langues : « no se pudo encontrar o cargar la clase principal » (« la classe principale n'a pas pu être trouvée ni chargée ») veut dire **que `java` a cherché une classe de ce nom et ne l'a pas trouvée** ; c'est presque toujours parce qu'il manque la compilation, parce que tu as mal écrit le nom ou parce que tu es dans un autre dossier. Le troisième message est le plus embrouillé des trois : `javac` a interprété `Hola` comme le nom d'une classe et non comme un fichier, et sa plainte parle de « traitement des annotations », un sujet qui n'a rien à voir avec ce qui t'est arrivé. La correction est toujours la même : `javac` prend le nom du **fichier**, avec son `.java`.

Pourquoi `java` cherche la classe là où il la cherche : par défaut, dans le dossier courant. Il existe un mécanisme pour lui dire de chercher ailleurs (le *classpath*, que la Leçon 6 présente et la Leçon 8 étudie), mais tant que tu es placé dans le dossier du `.class`, il le trouve.

#### 1.3.3 Le vieux `.class`

Le `.class` est une copie figée : **il ne se met pas à jour tout seul quand tu modifies le `.java`**. On l'a mesuré ainsi. On a compilé un programme qui imprime `version uno`, on l'a exécuté avec `java V`, on a changé le fichier pour qu'il imprime `version dos` et on l'a réexécuté avec `java V`, sans compiler :

```text
$ java V
version uno
```

Il dit toujours `version uno` (« version un »), alors que le `.java` dit déjà autre chose, parce que `java V` exécute le `.class` et que le `.class` n'a rien remarqué. Il faut recompiler avec `javac V.java`. En revanche, `java V.java` (avec l'extension) recompile à chaque exécution et reflète toujours le fichier actuel, ce qui est une autre raison pour laquelle le cours l'utilise dans les premières leçons. Le jour où le projet grandira, Maven se chargera de recompiler quand il le faudra, ce qui est l'une des choses pour lesquelles il existe (Leçon 6).

### 1.4 `jshell` : essayer une idée sans créer de fichier

Parmi les outils du JDK, il y en a un que tu utiliseras tous les jours : `jshell`, un interpréteur interactif ([manuel](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html)). Tu écris une expression, Java l'évalue et te montre le résultat, sans fichier, sans `main`, sans compiler. C'est la façon la plus rapide de répondre à « que fait ceci ? ». Dans le terminal, écris `jshell` :

```text
$ jshell
|  Welcome to JShell -- Version 25.0.4
|  For an introduction type: /help intro

jshell> 1 + 1
$1 ==> 2

jshell> String s = "hola"
s ==> "hola"

jshell> s.length()
$3 ==> 4

jshell> "142" + 1
$4 ==> "1421"

jshell> /vars
|    int $1 = 2
|    String s = "hola"
|    int $3 = 4
|    String $4 = "1421"

jshell> /exit
|  Goodbye
```

Cette session a été montée avec les réponses réelles d'une exécution de `jshell` avec ces mêmes commandes. Chaque fois que tu écris quelque chose à l'invite `jshell>`, il l'évalue et répond avec une flèche `==>` et la valeur. Les expressions qui n'ont pas de nom en reçoivent un automatique : `$1`, `$3`, `$4`. (Où est le `$2` ? L'instruction `String s = "hola"` a donné un nom à sa valeur, `s`, et c'est pourquoi le compteur a sauté.) La commande `/vars` liste les variables qui existent, avec leur type, et `/exit` quitte. Les commandes de `jshell` commencent par une barre oblique, pour les distinguer du code Java.

Remarque la quatrième entrée : `"142" + 1` a donné `"1421"`, le même résultat que tu as vu dans `fig00_04.java`. C'est tout l'intérêt de `jshell` : il sert à **vérifier** un doute en dix secondes. Il te montre aussi les erreurs sans bruit :

```text
jshell> int x = 5
x ==> 5

jshell> x = "a"
|  Error:
|  incompatible types: java.lang.String cannot be converted to int
|  x = "a"
|      ^-^

jshell> foo()
|  Error:
|  cannot find symbol
|    symbol:   method foo()
|  foo()
|  ^-^
```

Avec trois limites qu'il vaut mieux dire d'un coup. `jshell` ne remplace pas un programme : ce que tu y essaies n'est pas conservé (il y a `/save` pour cela, mais un vrai fichier vaut mieux). Certaines choses du langage qui ont besoin de contexte, comme un `main` ou une classe avec plusieurs méthodes, n'y sont pas commodes. Et la numérotation `$1`, `$2`... déroute au début. Utilise-le pour les doutes sur une expression et sur une méthode de la bibliothèque, pas pour écrire le `revisor`.

### 1.5 Lire la première erreur du compilateur

#### 1.5.1 La provoquer

Les erreurs du compilateur sont la première chose que tu liras chaque jour, il vaut donc mieux te familiariser avec elles dès maintenant. On en provoque une exprès. Ce programme a une faute de frappe : la méthode s'appelle `println`, et le programme écrit `prinln`. Enregistre-le dans `~/curso-java` sous le nom de la figure, `fig01_02.java`, pour que le message que tu verras dise la même chose que celui ci-dessous (le message nomme le fichier), et copie aussi le commentaire de la première ligne, parce que les numéros de ligne le comptent :

```java
// fig01_02.java
void main() {
  IO.prinln("hola");
}
```

```text
$ java fig01_02.java
fig01_02.java:3: error: cannot find symbol
  IO.prinln("hola");
    ^
  symbol:   method prinln(String)
  location: class IO
1 error
error: compilation failed
```

#### 1.5.2 Anatomie

C'est un message en six parties, et les six se répètent, avec de petites variantes, dans n'importe quelle erreur de compilation que tu verras de ta vie.

1. **`fig01_02.java:3:`** est le fichier et le numéro de **ligne**. La ligne 1 est le commentaire `// fig01_02.java`, la 2 est `void main() {` et la 3 est celle qui échoue. Quand l'erreur indique une ligne, c'est l'endroit où regarder d'abord ; parfois le vrai problème est un peu avant (dans une ligne précédente sans `;`), mais le compilateur ne s'en rend compte qu'ici.
2. **`error: cannot find symbol`** est le **diagnostic** : « le symbole est introuvable ». Un symbole est un nom : d'une variable, d'une méthode, d'une classe. Le compilateur a vu un nom et ne sait pas à quoi il se rapporte.
3. **La ligne citée**, `IO.prinln("hola");`, copiée du fichier pour que tu n'aies pas à aller la chercher.
4. **La flèche `^`**, qui pointe la colonne où le compilateur a détecté le problème. Ici elle est sous le point, à la frontière entre `IO` et ce qui suit : le compilateur sait que `IO` existe, et ce qu'il ne trouve pas, c'est ce qui vient après le point.
5. **`symbol:   method prinln(String)`** dit **ce qu'**il n'a pas trouvé avec autant de détail qu'il peut : une méthode appelée `prinln` qui reçoit un `String` (le texte). Remarque qu'il a déduit le type `String` de l'argument que tu as passé.
6. **`location: class IO`** dit **où** il l'a cherché : dans la classe `IO`. Ensemble, `symbol` et `location` se lisent comme une phrase : « j'ai cherché la méthode `prinln(String)` dans la classe `IO` et elle n'existe pas ». Et cette phrase te dit comment le corriger : soit elle n'existe pas parce que tu l'as mal écrite (c'est le cas), soit parce qu'elle n'est pas dans cette classe, soit parce qu'il manque un `import`.

À la fin, `1 error` compte combien il en a trouvé, et `error: compilation failed` est le lanceur `java` qui prévient que, de ce fait, il n'a rien exécuté. Si tu compiles avec `javac`, le message est le même mais sans cette dernière ligne, et `javac` termine avec le code de sortie 1.

#### 1.5.3 Comment on lit un compilateur

Il y a trois habitudes qui font gagner des heures. **Lis la première erreur, pas la dernière.** Souvent un seul problème produit une cascade : une accolade non fermée peut générer dix messages. Corrige la première, recompile et regarde combien restent. **Lis le message en entier avant de chercher sur internet.** `symbol` et `location` suffisent souvent. Et **quand tu ne comprends pas une erreur, réduis le programme** jusqu'à ce qu'il ne reste que la ligne qui la produit.

Cinq erreurs de plus, mesurées avec Temurin 25.0.4 avec des programmes de trois lignes et décrites ici pour que tu les reconnaisses quand tu les verras. Les messages sont abrégés à leur première ligne :

| Ce que tu as écrit | Ce que dit le compilateur |
|---|---|
| il manque le `;` à la fin : `IO.println("hola")` | `error: ';' expected` (avec la flèche juste après la parenthèse) |
| il manque la fermeture des guillemets : `IO.println("hola);` | `error: unclosed string literal` |
| `io.println("hola");` (minuscule) | `error: cannot find symbol` / `symbol:   variable io` / `location: class <nom du fichier>` |
| il manque l'accolade finale `}` | `error: reached end of file while parsing` |
| la méthode s'appelle `principal` et non `main` | `error: compact source file does not have main method in the form of void main() or void main(String[] args)` |

Le troisième mérite une lecture : `io` en minuscule n'est pas `IO` ; Java distingue les majuscules, donc le compilateur pense que `io` est une variable et ne la trouve pas. Le dernier dit en toutes lettres ce que Java attend d'un fichier compact : une méthode `main`, sans paramètres ou avec un seul, un `String[] args`.

### 1.6 Quand la version est la mauvaise

Cette section explique l'un des faux pas les plus courants chez ceux qui débutent avec Java. Le programme de 1.2, `Hola.java`, ne fonctionne que dans Java 25. Avec une version antérieure, les erreurs sont autres, et il vaut mieux les voir pour les identifier d'un coup d'œil. Avec **Java 21** (celui qu'installe `default-jdk` sur Mint 22), le même `Hola.java` donne ceci. (Ces deux blocs sont des fragments et non des figures du cours : ils ont été exécutés avec un autre JDK, le 21 et le 17, et c'est pourquoi on ne les compare pas avec Java 25.)

```text
$ java --version
openjdk 21.0.12 2026-07-21 LTS
OpenJDK Runtime Environment Temurin-21.0.12+8 (build 21.0.12+8-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12+8 (build 21.0.12+8-LTS, mixed mode, sharing)

$ java Hola.java
Hola.java:2: error: unnamed classes are a preview feature and are disabled by default.
void main() {
^
  (use --enable-preview to enable unnamed classes)
1 error
error: compilation failed
```

Il dit, en substance, que « les classes sans nom sont une fonctionnalité en préversion et sont désactivées ». Dans Java 21, le fichier compact existait, mais comme préversion (l'idée de la Leçon 0) : il fallait le demander avec `--enable-preview`, et sous un autre nom et une autre forme. Dans Java 25, il est désormais définitif. Avec **Java 17** :

```text
$ java --version
openjdk 17.0.20 2026-07-21
OpenJDK Runtime Environment Temurin-17.0.20+8 (build 17.0.20+8)
OpenJDK 64-Bit Server VM Temurin-17.0.20+8 (build 17.0.20+8, mixed mode, sharing)

$ java Hola.java
Hola.java:2: error: class, interface, enum, or record expected
void main() {
^
Hola.java:4: error: class, interface, enum, or record expected
}
^
2 errors
error: compilation failed
```

« On attendait une classe, une interface, une énumération ou un `record` » : Java 17 ne connaît pas le fichier compact, et attend que tout ce qui se trouve dans un fichier soit à l'intérieur d'une classe.

**Le diagnostic est toujours le même :** `java --version`. S'il dit 21 ou 17, ce n'est pas une erreur de ton programme, c'est la version. Pour corriger cela, installe Temurin 25 avec SDKMAN (1.1.5), ouvre un terminal neuf et, si tu en as plusieurs, choisis le bon avec `sdk default java` (1.1.7). L'essentiel : **ne modifie pas ton programme pour qu'il compile dans une vieille version**. Tout le cours est écrit pour Java 25.

### 1.7 Le style du code

Un dernier détail qui paraît mineur et ne l'est pas. Tu auras remarqué que le code de cette leçon utilise **deux espaces** d'indentation, pas quatre ni des tabulations. Ce n'est pas un caprice : c'est le guide de style de Google pour Java ([Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)), l'un des plus répandus, et c'est celui que vérifie l'outil Checkstyle dans la Leçon 10. Si le code des leçons 1 à 9 utilisait un autre style, la 10 devrait avouer que tout ce qui précède était mal écrit. Beaucoup de code Java sur internet utilise quatre espaces ; cela fonctionne pareil, et ce n'est qu'une convention. Le guide demande trois choses dès maintenant : une indentation de **2 espaces**, des lignes de **100 colonnes** au maximum, et des accolades toujours, même dans un `if` d'une seule ligne (quand tu y arriveras).

### 1.8 L'étape du projet : le premier fichier du `revisor`

Le `revisor` commence aujourd'hui. Il ne vérifie encore rien ; ce qui compte, c'est qu'il existe, au bon endroit, et qu'il s'exécute. Dans le dossier `~/curso-java/revisor/` que tu as créé en 1.2.1, crée le fichier `Revisor.java` avec ce contenu (tu peux omettre le commentaire de la première ligne ; il est là parce que, dans le dépôt, la figure s'appelle `fig01_03.java`) :

```java
// fig01_03.java
void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

Depuis `~/curso-java/revisor/`, exécute-le avec le nom de ton fichier :

```text
$ java Revisor.java
revisor: todavía no revisa nada
```

(Remarque le `í` de « todavía » : Java écrit l'accent sans problème, tant que ton terminal utilise UTF-8, ce qui est normal sous Mint. Si un jour tu vois `?` à sa place, c'est la configuration régionale du système et non ton programme ; la Leçon 0 l'a mesuré.)

Maintenant fais-le aussi à l'ancienne, pour voir le `.class` du projet :

```bash
javac Revisor.java
java Revisor
javap Revisor
```

Il imprime la même chose, et `javap` montre `final class Revisor`, avec la méthode `main` dedans. À la fin, **supprime le `.class`** avec `rm Revisor.class` : c'est un fichier à toi, que tu viens de générer, et il ne te sert plus, car à partir de maintenant la façon d'exécuter le projet est `java Revisor.java`, et un vieux `.class` dans le dossier est un piège (1.3.3) pour le jour où tu modifieras le `.java` et exécuteras le `.class` par habitude.

Le projet grandit à partir d'ici. Dans la Leçon 2, `Revisor.java` apprend à classer un service ; dans la 3, il se scinde en plusieurs fichiers ; dans la 6, il devient un projet Maven. Le `revisor` qui termine le cours démarre comme ce programme de trois lignes.

## L'erreur que tu vas voir

Cette leçon a deux erreurs principales. La première est celle de toujours : le compilateur qui ne trouve pas un nom. La seconde est celle de la mauvaise version, et c'est celle qui désoriente le plus ceux qui débutent.

**La première : `cannot find symbol`.** C'est celle de `fig01_02.java`, et elle se lit avec les six parties de 1.5.2 :

```text
fig01_02.java:3: error: cannot find symbol
  IO.prinln("hola");
    ^
  symbol:   method prinln(String)
  location: class IO
```

C'est presque toujours une faute de frappe, une majuscule erronée ou un `import` oublié. Elle se corrige en rectifiant le nom et en réexécutant. Ce qu'on ne fait **pas**, c'est supprimer des lignes jusqu'à ce qu'elle disparaisse : le message te dit exactement quel nom il ne trouve pas et où il l'a cherché.

**La seconde : le fichier compact avec une version antérieure.** Avec Java 21 :

```text
error: unnamed classes are a preview feature and are disabled by default.
  (use --enable-preview to enable unnamed classes)
```

Avec Java 17, `error: class, interface, enum, or record expected`. Ce sont les deux symptômes d'une seule maladie : **une version de Java antérieure à la 25**. Elle ne se corrige pas en touchant au programme mais à l'installation : `java --version` pour confirmer, et 1.1.5 et 1.1.7 pour corriger. C'est l'erreur de qui a installé `default-jdk` ou suit un vieux tutoriel.

**Une troisième, plus modeste :** `Error: Could not find or load main class Hola`. Elle veut dire que l'on a exécuté `java Hola` sans avoir compilé, ou depuis un dossier où le `.class` n'est pas. Compile avec `javac Hola.java`, ou exécute avec `java Hola.java`.

## Ce qui se fait de travers

**Installer `default-jdk` par habitude.** C'est la réponse la plus populaire et, sur Mint 22, elle installe Java 21, avec lequel le fichier compact échoue. Le chemin du cours est Temurin 25 avec SDKMAN (1.1.4 et 1.1.5).

**Ne pas vérifier la version après l'installation.** « Ça s'est installé sans erreurs » ne veut pas dire « la commande `java` pointe vers la 25 ». La vérification de 1.1.6 tient en deux commandes et dix secondes, et elle se répète chaque fois que tu changes quelque chose dans le système.

**Vérifier dans le même terminal où tu as installé.** C'est le piège mesuré de 1.1.6 : ce terminal continue d'utiliser le Java qu'il avait à son ouverture, et tu conclus que l'installation a échoué. Ouvre un terminal neuf avant de vérifier.

**Vérifier seulement `java` et non `javac`.** Un JRE a le premier et pas le second. Pour ce cours, il te faut les deux.

**Exécuter des scripts d'origines que tu ne connais pas.** `curl ... | bash` exécute ce qu'il télécharge, et c'est pourquoi ce qui compte, c'est d'où il vient : l'installateur de SDKMAN sort du site officiel du projet, par `https`, et peut être lu avant d'être exécuté (1.1.4). Un script collé depuis un forum ou depuis un site quelconque n'offre aucune de ces deux garanties. Il en va de même, a fortiori, pour toute commande avec `sudo`, qui modifie ton système avec des droits d'administrateur : si tu ne sais pas ce que fait chaque partie, ne l'exécute pas.

**Exécuter `java Hola` sans avoir compilé, ou modifier sans recompiler.** C'est le vieux `.class` de 1.3.3 : le programme fait ce que faisait la version précédente et tu crois que ton changement n'a pas fonctionné. Tant que tu apprends, utilise `java Hola.java`, qui compile toujours le fichier actuel.

**Mettre l'extension là où elle ne va pas, et la retirer là où elle va.** `java` avec un fichier source prend `.java` (`java Hola.java`) ; avec une classe compilée, pas d'extension (`java Hola`) ; et `javac` prend toujours `.java`. Ce sont trois règles, et elles s'apprennent par cœur à l'usage.

**Corriger à l'aveugle plusieurs erreurs à la fois.** Une erreur de compilation peut en générer d'autres qui n'existent pas vraiment. Corrige la première, recompile, et regarde ce qui reste.

**Écrire avec des tabulations ou avec quatre espaces « parce que tout le monde le fait ».** Cela compile pareil, mais dans la Leçon 10 l'outil de style se plaindra de chaque ligne. Configure ton éditeur avec deux espaces (dans Xed : Préférences, onglet Éditeur, largeur de tabulation 2 et insérer des espaces au lieu de tabulations).

**Suivre un tutoriel sans regarder sa date.** Si le tutoriel met `public class` et `public static void main` pour un programme d'un seul fichier, il date d'avant Java 25 ; cela fonctionne, mais ce n'est pas le style de ce cours. Et s'il parle de `default-jdk` ou d'un JDK antérieur, aussi.

## Exercices

### Exercice 1 — Ton premier programme, à ta façon

Modifie `Hola.java` pour qu'il imprime trois lignes : ton nom, le nom du cours et la phrase « Java 25 ». Exécute-le avec `java Hola.java`. Ensuite, compile-le avec `javac Hola.java`, exécute-le avec `java Hola` et compare. Enfin, change un mot du texte, **sans** recompiler, et exécute de nouveau `java Hola`. Note dans ton journal de bord ce qui s'est passé et pourquoi.

### Exercice 2 — Prédis et vérifie trois erreurs

Écris trois versions cassées de `Hola.java`, une à la fois : (a) sans le `;` à la fin du `IO.println` ; (b) sans fermer les guillemets du texte ; (c) avec `io.println` en minuscule. **Avant d'exécuter chacune, écris dans ton journal de bord quel message tu attends.** Exécute-les et compare. Pour chacune, indique la ligne que signale le compilateur et où pointe la flèche.

### Exercice 3 — Interroge `jshell`

Ouvre `jshell` et évalue : `"142" + 1`, `2300 / 1000` et `2300 / 1000.0`. Pour chacune, écris d'abord ce que tu crois qu'elle donne. Ensuite, tape `/vars` et sors avec `/exit`. L'une des trois réponses te surprendra probablement : écris dans le journal de bord laquelle et pourquoi tu crois qu'elle donne ce qu'elle donne. (L'explication complète est dans la Leçon 2 ; ici, ce qui compte, c'est de l'avoir vu.)

### Exercice 4 — Diagnostique par le message

Pour chaque message, dis ce qui s'est passé et comment le corriger, sans rien exécuter :

1. `Error: Could not find or load main class Hola` / `Caused by: java.lang.ClassNotFoundException: Hola`, en tapant `java Hola`.
2. `error: Class names, 'Hola', are only accepted if annotation processing is explicitly requested`, en tapant `javac Hola`.
3. `error: unnamed classes are a preview feature and are disabled by default.`, en tapant `java Hola.java`.

## Solutions

### Solution 1

Un `Hola.java` possible :

```java
void main() {
  IO.println("Dorian Chávez");
  IO.println("Curso de Java de Hábil");
  IO.println("Java 25");
}
```

Avec `java Hola.java`, il imprime les trois lignes. Après `javac Hola.java`, `java Hola` imprime la même chose. Si maintenant tu changes le texte d'un `IO.println` dans le `.java` et que tu exécutes `java Hola` **sans** recompiler, il imprime l'**ancien** texte : le `.class` est une copie figée (1.3.3) et ne sait pas que tu as changé la source. Pour voir le changement, il faut exécuter `javac Hola.java` de nouveau, ou utiliser `java Hola.java`, qui recompile toujours. Ce que tu notes dans le journal de bord : le `.class` ne se met pas à jour tout seul.

### Solution 2

Les trois messages, mesurés (chacun est montré avec sa ligne signalée ; le compilateur termine toujours par `1 error` et `error: compilation failed`) :

```text
a.java:2: error: ';' expected
  IO.println("hola")
                    ^

b.java:2: error: unclosed string literal
  IO.println("hola);
             ^

c.java:2: error: cannot find symbol
  io.println("hola");
  ^
  symbol:   variable io
  location: class c
```

En (a), le compilateur signale la ligne 2, pas une ligne plus loin, et place la flèche **juste après le dernier caractère** de `IO.println("hola")`, parce que c'est là qu'on attendait le `;`. En (b), la flèche pointe le guillemet d'ouverture : c'est là que commence le texte qui n'a jamais été fermé. En (c), la flèche pointe `io`, et le message dit `variable io`, pas `class io` : comme la minuscule ne correspond à aucun nom que Java connaisse, le compilateur suppose que c'est le nom d'une variable. Si la faute (c) t'a fait penser « il devrait dire que `io` n'existe pas », c'est exactement ce qu'il dit, avec le vocabulaire du compilateur. (Dans `location: class c`, le nom `c` est celui du fichier `c.java` avec lequel on a mesuré ; dans ton fichier, il dira le tien.)

### Solution 3

Les réponses de `jshell`, mesurées :

```text
jshell> "142" + 1
$1 ==> "1421"

jshell> 2300 / 1000
$2 ==> 2

jshell> 2300 / 1000.0
$3 ==> 2.3
```

Celle qui surprend est la deuxième : **`2300 / 1000` donne `2`, pas `2.3`**. Quand les deux nombres sont entiers, Java fait une division d'entiers et écarte la partie décimale. Pour obtenir `2.3`, l'un des deux doit être décimal, comme le `1000.0` de la troisième. C'est une affaire qui reviendra plus tard : le `revisor` convertit 2300 millisecondes en « 2.3s » et, s'il divisait des entiers, il afficherait `2s`. La Leçon 2 l'explique en détail. (Ici tu l'as vu de tes yeux, ce qui était l'important. La numérotation dans ta session peut différer selon ce que tu as évalué avant.)

### Solution 4

1. **On a exécuté la classe sans l'avoir compilée** (ou depuis un autre dossier). `java Hola` cherche un fichier `Hola.class` et il n'y en a pas. Cela se corrige en compilant d'abord (`javac Hola.java`) ou en exécutant directement le fichier source (`java Hola.java`).
2. **On a donné à `javac` le nom de la classe et non celui du fichier.** `javac` prend `.java` : `javac Hola.java`. Le texte sur le « traitement des annotations » embrouille, mais c'est la façon dont `javac` interprète un argument sans extension.
3. **La version de Java est antérieure à la 25**, probablement la 21 : le fichier compact sans `class` n'est définitif que dans Java 25. Cela se corrige en installant Temurin 25 avec SDKMAN (1.1.5), en le laissant par défaut avec `sdk default java` s'il y en a un autre (1.1.7), et en confirmant avec `java --version` dans un terminal neuf.

## Comment savoir que j'ai réussi

- [ ] Dans un terminal neuf, `java --version` imprime une première ligne qui commence par `openjdk 25` et une deuxième qui dit `Temurin`, et tu sais ce que signifiait le code 127 si tu n'avais pas Java avant.
- [ ] `sdk version` répond, et tu sais quel identifiant de Temurin tu as installé et pourquoi celui-là.
- [ ] `javac --version` imprime `javac 25` suivi d'un numéro de correctif.
- [ ] `java Hola.java` imprime ce que tu as écrit, et `javac Hola.java` laisse un `Hola.class` qui s'exécute avec `java Hola`.
- [ ] Tu peux expliquer pourquoi, si tu modifies le `.java` et exécutes `java Hola`, tu ne vois pas ton changement.
- [ ] Dans `jshell`, `1 + 1` donne `$1 ==> 2` et tu sais sortir avec `/exit`.
- [ ] Tu as provoqué l'erreur de `fig01_02.java` et tu peux y indiquer la ligne, la flèche, le `symbol` et le `location`.
- [ ] Depuis `~/curso-java/revisor/`, `java Revisor.java` imprime `revisor: todavía no revisa nada`.
- [ ] Tu as supprimé le `Revisor.class` que tu as généré, et tu sais pourquoi.
- [ ] Tu as noté dans ton journal de bord ce que t'a dit `java --version` sur ta machine et ce qui s'est passé avec l'Exercice 1.

## Résumé

Réponds sans regarder la leçon :

1. Pourquoi `default-jdk` sur Mint 22 ne convient-il pas pour ce cours ? Quel message produit le fichier compact avec cette version ?
2. Que fait `curl -s "https://get.sdkman.io" | bash`, et qu'est-ce qui le rend acceptable ici ? Comment choisis-tu l'identifiant de Temurin dans `sdk list java` ? Pourquoi faut-il ouvrir un terminal neuf après l'installation ?
3. Quelles deux commandes exécute-t-on pour vérifier l'installation, et pourquoi les deux ?
4. Qu'est-ce que le fichier compact ? Quelles deux choses du programme traditionnel ne sont plus nécessaires ?
5. Quelle différence y a-t-il entre `java Hola.java` et `java Hola` ? Quand le `.class` devient-il périmé ?
6. Que contient le `Hola.class` et comment l'as-tu vérifié avec `javap` ?
7. À quoi sert `jshell` et qu'est-ce qu'il n'est pas ?
8. Que disent les six parties d'un message comme `cannot find symbol` ?
9. Comment distingues-tu, uniquement par le message, l'erreur d'un programme de l'erreur d'une mauvaise version ?

## Pour aller plus loin

- [SDKMAN, installation](https://sdkman.io/install/) et [utilisation](https://sdkman.io/usage/) : les prérequis, l'installateur, et les commandes `list`, `install`, `use` et `default`. Consulté le 13 octobre 2026.
- [JEP 512, « Compact Source Files and Instance Main Methods »](https://openjdk.org/jeps/512) : la proposition qui définit le fichier compact, `void main()` et la classe `IO` ; elle explique ce que le compilateur crée pour toi et pourquoi. Consulté le 7 octobre 2026.
- [Oracle, manuel de `jshell` (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html) : toutes les commandes de `jshell`, de `/vars` à `/save`. Consulté le 7 octobre 2026.
- [Oracle, manuel de la commande `java` (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html) : le lanceur d'un fichier source et les options comme `--enable-preview`. Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 0 — Ce qu'est Java et ce qu'il n'est PAS](00-que-es-java.md) ·
**Suivante :** [Leçon 2 — Types, chaînes et contrôle de flux](02-tipos-cadenas-control.md)
