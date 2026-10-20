# CastleForge 0.3 — Work-Auftrag: Referenzburg vor Generatorvielfalt

Stand: 20.10.2026. **Dies ist ein freigegebener Entwicklungsauftrag, keine bereits implementierte Version 0.3.**

## 1. Auftrag und überprüfter Ausgangspunkt

Repository: `badmon61/castleforge`.
Arbeitsbranch: `feat/reference-castle-0.3`.
Implementierungsbasis: `740bcd699957f4cf36faf2820cccdc92e757be51` auf `feat/castle-identity-and-diagnostics` (Draft-PR #3), aufbauend auf `feat/terraced-architecture` (Draft-PR #2).

Bei Arbeitsbeginn Remote-Stand, PR-Zustand, Arbeitsverzeichnis und lokale Änderungen erneut prüfen. Nicht vom älteren `main` beginnen und dadurch die 0.2.x-Arbeit verlieren. Nicht blind auf einen alten Commit zurücksetzen. Neuere Änderungen bewerten und erhalten; bei unauflösbarem Konflikt anhalten.

Das technische Ziel dieses Auftrags ist eine neue, bewusst gestaltete, möblierte Referenzburg plus die dafür nötige schlanke Architekturmodellierung und visuelle Prüfkette. Das bestehende technische Fundament ist wiederverwendbar; die bisherigen Quadergrundrisse sind nicht der gestalterische Maßstab. Keine reine Konzeptabgabe und keine Metadatenversion als Ersatz für sichtbaren Architekturfortschritt.

Aktuell im Repository gepinnt: Minecraft Java `26.1.2`, Fabric Loader `0.19.5`, Fabric API `0.155.3+26.1.2`. Toolchain, Loom und Gradle anhand der Builddateien verifizieren. Diese Versionen sind die Projektbasis, keine Behauptung über die jeweils neuesten Releases. Keine ungefragten Versionswechsel. Einen Loader-Mindestwert nicht nur absenken, um eine Fehlermeldung zu umgehen: Kompatibilität nachweisen.

## 2. Nutzerfeedback und Grenzen der vorhandenen Nachweise

Der Nutzer hat reale Ingame-Screenshots des Prototyps auf einer Superflat-Welt geliefert und das Ergebnis gestalterisch abgelehnt. Beobachtet wurden leere, ähnlich große rechteckige Innenräume, Leiterzugänge, flache/monotone Wände, einfache Fenster und offen sichtbare Glowstone-Blöcke. Das Gebäude wirkt nicht wie die besprochene imposante, gewachsene Burg. Einige Fenster wirken durch benachbarte Bauteile eingeschränkt; die konkrete Ursache muss am Modell geprüft werden.

Die Screenshots belegen einen sichtbaren Bau in Minecraft, aber keine vollständige Abnahme von Navigation, Undo, Performance oder Modkompatibilität. Der exakte Seed und installierte Commit sind aus diesen Ansichten nicht belastbar dokumentiert. Frühere Aussagen über erfolgreiche reine Java-Tests sind kein Beleg für ästhetische Qualität.

Ein früherer Geländeversuch meldete `Terraced site rejected: No orientation fits dry terrain within the foundation limit`. Der aktuelle Kandidatenlauf verwirft mehrere Fehlerarten und gibt teilweise einen Sammelgrund aus. Daher nicht behaupten, der Screenshot beweise ausschließlich einen zu steilen Hang: unbekannter Boden, Wasser oder andere Standortprobleme sind gesondert zu diagnostizieren.

## 3. Verbindliche Arbeitsgrenzen

Erlaubt:
- Projektbezogene Code-, Asset-, Test- und Dokumentationsänderungen in diesem Arbeitsbranch.
- Builds, lokale reine Generatorprüfungen und projektisolierte Entwicklungs-Testwelten, soweit die Umgebung und vorhandenen Berechtigungen das zulassen.
- Kleine überprüfbare Commits, Push ausschließlich auf den Arbeitsbranch, Aktualisierung des zugehörigen Draft-PR.
- Notwendige Abhängigkeiten aus offiziellen Quellen innerhalb der freigegebenen Entwicklungsumgebung; keine zusätzlichen kostenpflichtigen Dienste.

Nicht ohne separate Freigabe:
- Merge, Auto-Merge, Rebase/Force-Push anderer Branches, Release-Veröffentlichung oder Upload auf Modplattformen.
- Änderungen an bestehenden Prism-Instanzen, insbesondere `Adventura 26.1.2` und `ElFadi 26.1.2`, bestehenden Welten, Servern, Shadern, Ressourcenpaketen oder Modlisten.
- Allgemeiner PC-/Browser-Zugriff, Installation systemweiter Software oder Änderungen an Benutzerkonten/Authentifizierung.
- EULA- oder sonstige Zustimmungserklärungen im Namen des Nutzers; vorhandene Zustimmung prüfen, fehlende als Blocker melden.
- Fremde Schematics, Texturen, Baupläne oder Minecraft-Binaries in das Repository kopieren. Nur eigene Bauteile oder ausdrücklich freigegebene Assets mit belegten Rechten.

Work wird nicht live durch den anderen Chat überwacht. Die Begleitung erfolgt durch diesen Auftrag und einen Abschluss-/Blockerbericht, den der Nutzer in den Hauptchat zurückgibt. Nicht behaupten, Informationen seien automatisch zwischen Sitzungen übergeben worden.

## 4. Erster Meilenstein: eine Referenzburg

Zuerst eine gepflegte, historisch inspirierte mitteleuropäische Burg mit zurückhaltenden Fantasy-Akzenten. Arbeitstitel `Falkenwacht`, keine Zusage eines historisch korrekten Nachbaus. Die Qualität muss mit Vanilla-Texturen und ohne Shader erkennbar sein. Andere Kulturen und ruinierte Varianten folgen erst nach Abnahme dieser Gestaltung.

Gestalterische Vorgaben:
- Asymmetrische, aber zusammenhängende Gesamtkomposition; nicht drei isolierte Reihen aus gleichartigen Häusern.
- Ein klar dominanter Bergfried, mindestens ein niedrigerer Wohn-/Saalflügel, ein erkennbares Torhaus, unterschiedlich hohe Wehr-/Treppentürme, Ringmauer und ein nutzbarer Innenhof.
- Bewusste Unterschiede in Höhe, Breite, Dachsilhouette und Funktion. Freiflächen und Blickachsen nicht restlos zubauen.
- Als Entwurfsrahmen ungefähr 120–160 mal 80–120 Blöcke, dominanter Turm ungefähr 35–50 Blöcke hoch. Das sind veränderbare Designziele, keine Rechtfertigung, existierende Ressourcenbudgets blind zu erhöhen.
- Fassaden mit Sockel, Wandfeldern, zurückgesetzten Öffnungen, Fensterrahmen, Stürzen/Bögen, Gesimsen und Dachüberständen. Verwitterung zunächst sparsam und begründet, keine zufällig verstreuten Moosflecken als Detailersatz.
- Fenster vor der Platzierung gegen den Nachbarraum und Außenraum prüfen. Unbeabsichtigt vermauerte Fenster, sich schneidende Dächer und durch Korridore laufende Stützen sind Fehler.
- Dächer mit erkennbarer Tragstruktur, passend angeordneten Firsten und Übergängen; Rund- oder abgeschrägte Türme als bewusste Bauformen statt nur Höhe ändern.

Innenräume müssen eine erkennbare Funktion erhalten. Mindestens Saal/Speiseraum, Küche, Schlaf-/Wohnbereich, Lager, Wach-/Waffenraum und Bibliothek/Arbeitszimmer; Räume dürfen sinnvoll in zusammenhängenden Flügeln liegen. Saal mit größerer Deckenhöhe, Balken/Gewölbe, Tisch und Sitzplätzen. Küche mit Arbeitsflächen und plausibler Feuerstelle ohne unkontrollierte Feuer-/Lavaquelle. Keine riesigen unmöblierten Hauptzimmer.

Hauptetagen durch echte Treppen mit Podesten verbinden. Leiterzugänge nur für Neben-/Wartungsräume. Hauptwege vorzugsweise drei Blöcke breit und drei Blöcke lichte Höhe; Treppen mindestens zwei Blöcke breit. Bei Details dürfen begründete Abweichungen vorkommen, aber alle wesentlichen Räume müssen ohne Flug, Blockabbau oder Türme-Hochspringen zugänglich sein.

Beleuchtung als gestaltetes Bauteil: Laternen, geschützte Lichtquellen, Leuchter oder passende Wandleuchten. Kein offen eingelassener Glowstone als Standardlösung. Nicht bloß Glowstone durch Laternen ersetzen und den gleichen leeren Raum behalten.

## 5. Technische Umsetzung — so viel Modell wie nötig, kein Framework um seiner selbst willen

Empfohlene Trennung:
1. **Architekturmodell:** Gebäude, Raumfunktionen, Etagenhöhen, Türen, Treppen, Höfe, Dachkörper und Verbindungspunkte.
2. **Bauteilkatalog:** Eigene parametrisierte Fassadenfelder, Fenster, Bögen, Türme, Dachanschlüsse, Treppen und Möbelgruppen; kleine datengetriebene Definitionen dort, wo sie tatsächlich helfen.
3. **Reservierungs-/Prüfmodell:** Belegte Volumen, freizuhaltende Wege, notwendige Stützen und ausdrücklich erlaubte gemeinsame Anschlüsse.
4. **Blockübersetzung:** Materialrollen plus konkrete Blockzustände, ausgerichtete Treppen/Platten/Zäune/Laternen; mehrteilige Blöcke vollständig und rotationsrichtig setzen.
5. **Fabric-Platzierung:** Bestehende Bedienung und Schutzprüfungen nutzen, ohne die alten Generatoren stillschweigend zu verändern.

Neue Architektur in einem eigenen Package/Modul kapseln, den alten Code zunächst als Rückfall/Regression erhalten. Eine neue Vorschauvariante, etwa `/castleforge preview reference`, integrieren; den tatsächlich gewählten Befehl in Tests und README exakt dokumentieren. `/preview`, `/preview terraced`, `/inspect`, Bestätigung, Status, Abbruch und die bestehenden Undo-Grenzen dürfen nicht unbemerkt brechen.

`CultureMap`, `WorldSettings`, bestehende Kultur-IDs, Seed-Verteilung und gespeicherte 7×-Skalierung erhalten. Die automatische Biomgrößenmessung ist weiterhin nicht implementiert und nicht Teil dieses Meilensteins. Neue architektonische Varianten brauchen eine eigene stabile Versions-/Variantensignatur.

### Konkrete Risiken aus der Ausgangs-Codeprüfung

- `TerracedCastlePlanner.Builder.put` schreibt bisher mit `Map.put` den letzten Zustand pro Koordinate. Das dedupliziert, erkennt aber keine unbeabsichtigte Kollision zwischen zwei Modulen. Für neue Architektur Besitz/Schreibphasen und erlaubte Überlappungen nachweisbar unterscheiden; nicht nur ein größeres `HashMap` verwenden.
- `PlanNavigation` prüft bislang ein vereinfachtes Vollwürfelmodell und deklariert Ziele, nicht alle tatsächlichen Minecraft-Kollisionsformen. Treppen, Platten, Zäune, Teppiche, Möbel und Türen nicht als beliebige Vollwürfel abhandeln. Hauptwege müssen auch nach Einrichtung und Detaildurchlauf bestehen bleiben. Kein Weg über ein Dach als Ersatz für ein fehlendes Treppenhaus.
- `Palette` setzt `LIGHT` bisher auf Glowstone. Die neue Architektur braucht explizite, tragfähige Lichtbauteile mit korrekter Befestigung und Zuständen.
- Der aktuelle Undo-Job überspringt Koordinaten mit Block-Entities und speichert nur Blockzustände. Kisten, Fässer, Öfen, Betten oder andere datenhaltige Einrichtung deshalb nicht ohne gezielte Unterstützung hinzufügen. Entweder zunächst passende Einrichtung ohne Block-Entities verwenden oder sichere Eigentums-/Zustandsprüfungen und Wiederherstellung implementieren. Vom Spieler nachträglich eingefüllte Inventare oder geänderte Daten niemals beim Undo löschen. Abbruch/Undo inkl. Nachbarupdates im echten Spiel prüfen.
- Türen, Betten und andere mehrteilige Blöcke brauchen Gruppen-/Reihenfolgenbehandlung; Laternen, Kerzen, Teppiche und ähnliche Bauteile dürfen nicht vor ihren Stützen gesetzt werden. Keine schwebenden Möbel durch falsche Platzierungsreihenfolge.
- Die Vorschau ist bisher synchron. Ein größeres Bauwerk nicht mit ungeprüft erhöhten Budgets auf dem Serverthread planen. Chunk-Abfragen nur im zulässigen Thread, einen begrenzten unveränderlichen Snapshot erfassen und reine Planungsarbeit möglichst separat ausführen. Vor Schreiben erneut Konflikte prüfen; zeitliche/mengenmäßige Limits, Abbruch und Versionsprüfung vorsehen.

## 6. Sichtbarer Entwicklungsnachweis vor dem nächsten Nutzer-Test

Eine automatisierte Vorschau aus den **tatsächlich erzeugten Blockdaten** ist Pflicht. Mindestens Grundriss, Gesamtansicht aus zwei Richtungen, Fassade/Torhaus sowie Innenansichten/Schnitte von Saal und Treppenhaus.

Bevorzugt reproduzierbare Minecraft-Client-Screenshots in einer isolierten Entwicklungswelt. Alternativ zunächst ein aus echten Blockdaten abgeleiteter Renderer/Viewer mit Grundriss und Schnittebenen. Dabei ausdrücklich unterscheiden:
- Minecraft-Screenshot: echte Spielansicht; Modversion, Seed/Variante und Bildbedingungen dokumentieren.
- Technische Geometrievorschau: zeigt den Plan, prüft aber nicht automatisch Texturen, Licht, Physik oder tatsächliche Kollisionsformen.
- Konzeptzeichnung: rein gestalterische Idee, kein Implementierungsbeleg.

Keine generierten Konzeptbilder, Webbilder oder fremden Bauten als Nachweis der eigenen Umsetzung. Keine Minecraft-Texturen oder Spielassets ins Repo einchecken. Falls die Umgebung keine Client-Grafik zulässt, den konkreten Blocker melden und einen eng begrenzten lokalen Testauftrag vorbereiten. Nicht so tun, als seien zusätzliche automatisierte Tests ein Ersatz für die fehlende Ansicht.

Visuell iterieren: Erzeugen → ansehen → konkrete Fehler notieren → ändern → dieselben Ansichten erneut erzeugen. Nicht eine weitere JAR allein aufgrund eines erfolgreichen Compilers als schön oder fertig bezeichnen. Der Nutzer entscheidet über die gestalterische Abnahme.

## 7. Gelände — Schnittstelle jetzt, Bergintelligenz danach

Den Referenzbau zuerst auf flachem und sanft gestuftem Testgelände ausarbeiten. Diese bewusste Reihenfolge ist keine Änderung des Langzeitziels: Gebäude sollen später an Hängen, auf Felsvorsprüngen und über Klüften entstehen.

Im neuen Modell Bauabschnitte, eigene Höhen, Stützkontakte und Übergänge vorsehen. Standortfehler mit nachvollziehbaren Gründen und Koordinaten melden: ungeladen, Wasser, unbekannter Boden, geschützter Block, lokale Höhendifferenz, Fundamenttiefe, Kollision oder fehlender Zugang. Keine pauschale Behauptung, jeder abgelehnte Standort sei zu steil.

Nicht die 8-Blöcke-Grenze lediglich auf einen großen Wert setzen, nicht den ganzen Berg aushöhlen und nicht eine riesige massive Grundplatte bis zur höchsten Probe auffüllen. Automatische Generierung beim Erkunden bleibt ausgeschaltet. Keine destruktive Nachgenerierung in bestehenden Chunks.

Nach visueller Freigabe separat: Grundstücksuche, Kontur-/Terrassengraph, kleinere Bauabschnitte entlang des Hangs, Brückenplanung, echte Chunk-Worldgen-Integration und große Monumente. Bewohner, Bedrock/NeoForge, Straßen und neue Kultur-Lore nicht in diesen ersten Work-Auftrag hineinziehen.

## 8. Überprüfung und Abnahmekriterien

Vor Änderungen Basisbuild und vorhandene drei Prüfsuiten ausführen; genaue Befehle, Java/Gradle-Versionen, Commit und Ergebnis protokollieren. Veraltete Statusangaben der PRs sind kein aktueller Testlauf.

Neue Prüfungen mindestens:
- Identische Eingaben liefern identische architektonische Modelle und Blockpläne; vier Rotationen, negative Koordinaten und Grenzen.
- Kollisionen zwischen Modulen, mehrteilige Blockzustände, Stützen, Dachanschlüsse und Freiräume; absichtlich fehlerhafte Beispiele müssen abgelehnt werden.
- Zugang vom Haupttor zu jedem Hauptzimmer über vorgesehene Innenwege und Treppen, ohne Flug/Abbau/Leitern auf den Hauptrouten; erneute Prüfung nach Möblierung.
- Fenster haben einen gewollten Anschluss nach außen oder zu einer ausdrücklich geplanten Galerie; keine zufällig verbauten Öffnungen.
- Repräsentative gültige/ungültige Geländeproben; Abweisung verändert keine Weltblöcke.
- Bisherige Generator-/Kultur-Regressionen bleiben bestehen. Tests nicht lockern, nur um die neue Ausgabe grün zu bekommen. Änderungen an Golden-Dateien fachlich begründen.
- Fabric/GameTests, soweit mit der gepinnten Version umsetzbar: Anbringung und Rotation, Treppen-/Türkollision, Nachbarupdates, Bestätigung/Abbruch, Undo und Block-Entity-Konflikte.
- Separater Starttest ohne Clientklassen auf dem Dedicated Server; echtes Client-Rendering gesondert dokumentieren.
- Messbare Planungszeit, Snapshotzeit, maximale gleichzeitige Speicherbelegung soweit messbar, Block-/Block-Entity-Zahlen und Platzierungs-Tickzeiten. Keine FPS-Zusage anhand von Kernprüfungen. Eine Zahl dokumentieren, statt einfach höhere Limits zu setzen.

Mindestens ein fester Referenzseed und mehrere bewusst begrenzte Detailvarianten testen. Eine gute bewusst entworfene Burg ist in diesem Meilenstein wichtiger als 100 schlechte Zufallsvarianten. Erst nach Qualitätsfreigabe größere Variantenmatrizen.

Ergebnis gilt nur dann als visuell abnahmebereit, wenn echte Ansichten vorliegen, Hauptzimmer eingerichtet sind, Treppen und Zugänge funktionieren und bekannte Blocker offen genannt werden. Erfolgreiche Kompilierung, reine Geometrieprüfung, Serverlauf, Clientansicht und Nutzerfreigabe sind getrennte Zustände.

## 9. Arbeitsfolge und Stoppstelle

1. Branch/Zugriff/Basisversionen prüfen, vorhandene lokale Änderungen sichern, kurzen Ist-Befund notieren.
2. Referenzgrundriss und minimal nötiges Datenmodell festlegen; danach direkt implementieren, nicht mit einem weiteren langen Plan abschließen.
3. Bauteile, eingerichtete Räume, Treppen, Blockzustände, Kollisions-/Freiraumverträge und neue Vorschauvariante entwickeln.
4. Tests und tatsächliche Vorschauansichten erzeugen, ansehen und verbessern.
5. Den vollständigen Build ausführen, Artefakt prüfen, Versionsmetadaten und SHA-256 dokumentieren. Nur nach tatsächlicher Funktionsänderung die neue Version `0.3.0-alpha.1` vergeben; nicht diesen Dokumentationsstand als neue Mod ausgeben.
6. Kleine Commits pushen, Draft-PR mit Nachweisen aktualisieren und einen Bericht für den Hauptchat liefern.
7. **Dann stoppen:** Kein Merge, keine Veröffentlichung, keine Installation in bestehenden Nutzerinstanzen. Der Nutzer bringt Bericht/Ansichten in den Hauptchat zur Abnahme. Bei fehlenden Berechtigungen, EULA oder nötigen Änderungen außerhalb des Projekts früher stoppen und genau die kleinste erforderliche Freigabe nennen.

Der Abschlussbericht enthält: Branch, Commit, PR, geänderte Dateien/Systeme; tatsächlich ausgeführte Tests und Ausgaben; Ansichten und deren Herkunft; exakten Testbefehl; JAR/Hash sofern erfolgreich gebaut; bekannte Grenzen; offene Entscheidungen und gegebenenfalls den kleinsten notwendigen lokalen Work-Auftrag. Nichts als erledigt markieren, nur weil es geplant wurde.

## 10. Quellen und Versionstreue

Primär maßgeblich sind die genannten Quelldateien am Basiscommit und die aktuelle Implementierung. Die aktuelle Fabric-Anleitung unterscheidet reine Unit-Tests von GameTests und beschreibt Client-Screenshots. Sie kann eine andere Minecraft-Version als das Projekt anzeigen; APIs gegen die gepinnte 26.1.2-Umgebung prüfen, nicht blind kopieren.

- Fabric, Automated Testing: https://docs.fabricmc.net/develop/automatic-testing
- OpenAI, Sandbox / Arbeitsbereich und Freigaben: https://developers.openai.com/codex/sandboxing
- OpenAI, Worktrees: https://developers.openai.com/codex/environments/git-worktrees

Dieses Dokument enthält selbst entworfene Designziele und technische Anforderungen. Es behauptet nicht, dass die Referenzburg, ein neuer Renderer, GameTests oder Version 0.3 bereits implementiert wurden.
