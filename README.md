# GeneratePersonThatDoesNotExist

Based on website :``https://thispersondoesnotexist.com``

## Usage

```
usage: Generator
-help                Affichage de l'aide.
-n,--number <arg>    Nombre de photos a generer [defaut:1]
-o,--output <arg>    [REQUIRED] Spécifier un répertoire de sortie
-q,--quality <arg>   Qualité entre 0.0 et 1.0 (impact la taille du fichier) [defaut:0.5]
```

## Build and launch
```
cd <repertoire projet>
mvn clean dependency:copy-dependencies package
java -cp target\GeneratePersonThatDoesNotExist-1.0.jar com.pingouincorp.Generator -n 10 -o c:\temp\GeneratePersons
```