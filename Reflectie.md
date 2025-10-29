
## Voldoet aan:
Ik heb alle verplichte onderdelen van de ICSS tool afgemaakt.

De code bouwt gewoon met Maven en draait zonder fouten in IntelliJ met OpenJDK. Alles zit netjes in de juiste packages, precies zoals in de startcode (parser, checker, transform, generator, datastructures, enz.).
### Parser
De parser werk volledig. Hij leest ICSS-code in, maakt er een AST van en ondersteunt:

- gewone CSS- egels
- variabelen en het gebruiken ervan
- rekensommen met +, - en * met de juiste rekenvolgorde
- if en else statements

De parser gebruikt mijn eigen HANStack implementatie om de parent nodes bij te houden.

Alle voorbeeldbestanden (level0 t/m level3) worden goed geparsed.

### Checker
De checker controleert de betekenis van de code (dus of alles klopt qua types en variabelen).
Hij controleert:

- of een variabele bestaat voordat hij wordt gebruikt
- of de types in berekeningen kloppen (bijvoorbeeld geen pixels bij percentages optellen)
- of je geen kleuren gebruikt in rekensommen
- of properties het juiste type krijgen (bijv. width moet een pixel of percentage zijn, en color een kleur)
- of de if conditie een boolean is
- en of variabelen alleen binnen hun eigen scope worden gebruikt

Als er iets fout is, wordt dat opgeslagen in de AST met `setError()`.

iK gebruik een ` HANLinkedList<HashMap<String, ExpressionType>>`  om scoping bij te houden.
Dat maakt het makkelijk om een nieuwe scope te openen bij bijvoorbeeld een nieuwe `Stylerule` of `IfClause`.

De checks zelf zijn opgebouwd rond één functie `typeOf(Expression e)`, zodat ik overal consistent type informatie gebruik.
Dat voorkomt dubbel werk en houdt de code overzichtelijk.

Ik had losse lijsten of globale variabelen kunnen gebruiken om types bij te houden,
maar dat wordt al snel onoverzichtelijk bij geneste scopes. De keuze voor een stack met hashmaps is beter uitbreidbaar (als er later nieuwe functies toegevoegd moeten worden).

### Transformer
De evaluator vereenvoudigt de AST.
Hij berekent alle expressies en vervangt ze door de echte waarden, bijvoorbeeld `width: ParWidth + 20px;`  wordt `width: 520px;`.
Ook werkt hij de if else structuren af. Alleen de juiste body (if of else) blijft over, de rest wordt verwijderd.

Ik gebruik een scope stack met `HashMaps` om variabelen op te slaan tijdens het evalueren.
Bij `IfClause` wordt de conditie geëvalueerd met `eval()`, en daarna vervang ik de hele `IfClause` door de juiste body.

De transformer had ik in losse passes kunnen doen (eerst variabelen, dan if, dan berekeningen), maar dat zou meerdere keren door de boom lopen, en is dus meer gevoelig op fouten.

### Generator
De generator maakt van de AST echte CSS code.
Het gebruik van `StringBuilder` zorgt voor het bijhouden van de diepte van recursie.
De generator zoekt alleen naar `Declaration` nodes en ze om naar CSS.
Hij loopt de boom door en bouwt daar een CSS string van.
De output is gewone CSS, bijvoorbeeld:
```css
p {
  width: 500px;
  color: #124532;
  background-color: #000000;
  height: 20px;
}
```