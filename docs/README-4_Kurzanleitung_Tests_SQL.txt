README-Kurzanleitung

Zum Ausführen der Tests im Projektordner folgenden Befehl eingeben:

mvn clean verify

Danach warten, bis Maven den Build, den Test-WildFly und alle Unit- und Integrationstests ausgeführt hat. 
Am Ende der Konsolenausgabe werden die Testergebnisse automatisch zusammengefasst und aufgelistet.

Die Testergebnisse mit den SQL-Optimierungen sind zusätzlich in der Datei testergebnisse-4.txt dokumentiert.



!Warum Maven-Plugins verwendet werden!

Die Maven-Plugins werden genutzt, um den kompletten Build- und Testablauf reproduzierbar über einen einzigen Maven-Befehl
ausführen zu können. Das maven-war-plugin baut die normale WAR-Datei und zusätzlich die spezielle Integrationstest-WAR. 
Das maven-failsafe-plugin führt die Integrationstests aus, während das maven-surefire-plugin für die normalen Unit-Tests 
zuständig ist. Das wildfly-maven-plugin startet für die Integrationstests automatisch einen WildFly-Testserver, konfiguriert 
die H2-Datenquelle, aktiviert das Hibernate-SQL-Logging für die SQL-Zählung, deployed die Test-WAR und fährt den Server nach 
den Tests wieder herunter. Das exec-maven-plugin startet am Ende die eigene Ergebnis-Ausgabe, damit die Test- und SQL-Ergebnisse 
übersichtlich in der Konsole erscheinen.

Zusätzlich wurde eine dynamische Portwahl implementiert. Wenn die Standardports 8080, 8443 und 9990 bereits belegt sind, 
verwendet der Testserver automatisch die Alternativports 8180, 8543 und 10090. Dadurch können zum Beispiel eine normale 
WildFly-Instanz und der Integrationstest-WildFly parallel laufen, ohne dass sich die Ports gegenseitig blockieren. 
Falls sowohl die Standardports als auch die Alternativports belegt sind, bricht der Build mit einer klaren Fehlermeldung ab.



Hinweis zu Selenium- und HtmlUnit-Tests

Die Selenium- und HtmlUnit-Tests sind nicht immer direkt 1:1 vergleichbar, weil sie teilweise unterschiedliche Abläufe messen. 
Ein Beispiel dafür ist das Produktlöschen.

Kurz gesagt:

Selenium-Produkttest:
- löscht ein bereits vorher angelegtes Produkt
- misst hauptsächlich den Delete-Ablauf

HtmlUnit-Produkttest:
- legt bei Bedarf innerhalb des Delete-Tests erst ein eigenes Produkt an
- misst dadurch Create + Seitenaufrufe + Delete + Prüfung

Deshalb sollen Selenium- und HtmlUnit-Ergebnisse getrennt betrachtet werden. Die Gesamtzahl der SQL-Statements kann weiterhin 
angegeben werden, für die Bewertung einzelner Testfälle ist aber wichtig, ob Selenium oder HtmlUnit gemessen wurde.


Die HtmlUnit-Tests sind bewusst als schnellere Ergänzung zu den Selenium-Tests umgesetzt. Sie prüfen viele gleiche fachliche Bereiche, 
führen aber nicht immer exakt dieselben Browserinteraktionen aus. Selenium testet stärker echte UI-Abläufe im Browser, während HtmlUnit 
schneller Seitenaufrufe, Formulare und Tabelleninhalte prüft. Deshalb werden die SQL-Werte getrennt nach Selenium und HtmlUnit ausgegeben. 
Ein Beispiel ist das Produktlöschen: Selenium löscht ein bereits vorher angelegtes Produkt, während HtmlUnit bei Bedarf innerhalb desselben 
Tests erst ein eigenes Löschprodukt anlegt und danach löscht.