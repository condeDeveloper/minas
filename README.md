# minas

Campo minado em Java 21, com a pergunta trocada: não é como jogar, é **o que dá para provar**
com o que está na tela.

A regra que todo mundo usa jogando olha um número por vez: "esse 3 já tem três bandeiras, o
resto em volta é seguro". Aqui essa regra é escrita, medida, e depois confrontada com um juiz
que enumera **todas** as distribuições de minas que explicam a tela inteira ao mesmo tempo.

A diferença entre as duas é grande o bastante para mudar o jogo: no tabuleiro de especialista,
a regra local ganha 3 partidas em 100 e a enumeração ganha 51.

## O que tem aqui

- `Campo`: onde as minas estão de verdade. Sorteio com a primeira casa e os oito vizinhos
  dela limpos, que é a regra de todo campo minado moderno.
- `Visao`: o que quem joga enxerga. Números abertos, bandeiras e o total de minas anunciado.
  Toda dedução sai daqui, e só daqui.
- `RegraLocal`: as duas regras de olhar um número por vez, rodando até parar de descobrir.
- `Enumeracao`: o juiz completo. Separa a fronteira em pedaços independentes, enumera cada
  pedaço com poda, junta os pedaços com as casas do escuro através do total de minas e devolve
  a chance **exata** de cada casa, em contagem inteira.
- `ForcaBruta`: o juiz burro. Monta todo campo possível e confere cada um inteiro, sem saber o
  que é fronteira. Não serve para jogar, serve para dizer se a `Enumeracao` está certa.
- `Jogador`: joga a partida inteira sozinho, com quatro jeitos diferentes.
- `Casos` e `Medidor`: as posições mostradas e as tabelas.

## Como rodar

```bash
mvn -B verify
mvn -q exec:java -Dexec.args="confronto"   # a enumeração contra a força bruta
mvn -q exec:java -Dexec.args="vitorias"    # quantas partidas cada jeito ganha
mvn -q exec:java -Dexec.args="travadas"    # onde a regra local empaca
mvn -q exec:java -Dexec.args="custo"       # o tamanho da enumeração
mvn -q exec:java -Dexec.args="casos"       # posições achadas jogando
```

## Seis achados

### 1. Deduzir direito vale 17 vezes mais partidas no especialista

```
nivel                 jeito                    ganhas  sem chute  chutes  explodidos
iniciante (400)       REGRA_LOCAL               323        250     287          77
iniciante (400)       REGRA_COM_TOTAL           323        254     283          77
iniciante (400)       ENUMERACAO                383        360      50          17
iniciante (400)       ENUMERACAO_E_CHUTE_BOM    386        360      63          14
intermediario (200)   REGRA_LOCAL                93         41     374         107
intermediario (200)   REGRA_COM_TOTAL            93         41     374         107
intermediario (200)   ENUMERACAO                163        128     105          37
intermediario (200)   ENUMERACAO_E_CHUTE_BOM    168        128     110          32
especialista (100)    REGRA_LOCAL                 3          0     385          97
especialista (100)    REGRA_COM_TOTAL             3          0     385          97
especialista (100)    ENUMERACAO                 35         16     210          65
especialista (100)    ENUMERACAO_E_CHUTE_BOM     51         16     270          49
```

3 vitórias contra 51, no mesmo conjunto de 100 partidas e com a mesma primeira jogada. No
iniciante a diferença é de 81% para 96%, e parece pequena; no especialista é a diferença entre
o jogo ser impossível e o jogo ser difícil.

### 2. O total de minas vale muito, mas não como regra

Olhando a tabela acima: `REGRA_LOCAL` e `REGRA_COM_TOTAL` ganham **exatamente o mesmo número
de partidas** nos três níveis. A segunda acrescenta as duas contas óbvias do total ("já marquei
todas, o resto é seguro" e "o que falta é tudo o que sobrou") e isso rendeu 4 partidas a mais
terminadas sem chute, no iniciante, e nada em lugar nenhum.

Agora, o mesmo total **dentro da enumeração**:

```
nivel          posicoes  trava  destravadas  casas a mais  so pelo total  destravadas so pelo total
iniciante          1334    142           79          3059           1494                         7
intermediario      1489    165           63          4142            657                         7
especialista       2069    414          175          6041            339                         7
```

No iniciante, **1.494 das 3.059 casas** que a enumeração prova a mais que a regra local só saem
porque o número total de minas entrou na contagem. Quase metade. A mesma informação que, como
regra, não vale nada.

A coluna da direita diz quantas vezes o total foi o único motivo de a posição destravar. Deu 7
nos três níveis, o que é coincidência do tamanho das três amostras: com outros tamanhos dá
outros números.

### 3. A fronteira não explode

O medo óbvio é que enumerar seja impossível num tabuleiro de verdade.

```
nivel          posicoes  maior pedaco  maior enumeracao  media de pedacos
iniciante          1334            33              3430              1.54
intermediario      1489            63              4278              2.31
especialista       2069            81             35478              3.14
```

O maior pedaço de fronteira que apareceu teve **81 casas**. São 2^81 maneiras de espalhar
minas por elas, um número de 25 algarismos. As restrições deixaram **35.478** de pé. É por isso
que dá para fazer isso a cada jogada, sem esperar nada.

### 4. Chutar no escuro pode ser melhor que chutar na fronteira

Posição achada jogando, 240 casas fechadas e 39 minas soltas:

```
   ?...............
   ................
   ................
   ................
   ................
   ................
   ......2.........
   ......2112......
   .......1 2......
   .......312.4....
   ......F..32.....
   ...........3....
   ................
   ................
   ................
   ................
```

Nada está provado. Toda casa que não encosta em número nenhum tem a mesma chance, e o `?`
marca a primeira delas: **12,7%** de mina, contra **13,6%** da melhor casa da fronteira. A
intuição manda chutar perto dos números, onde se sabe alguma coisa; a contagem manda o
contrário, e nesta posição o escuro é o lugar mais seguro do tabuleiro. (Não havia mina.)

### 5. Uma posição em que só o total de minas resolve

Partida 2 do iniciante, fim de jogo, 28 casas fechadas e 3 minas soltas:

```
   oo.21 111
   oo.F1 2F2
   ooo3212F2
   oo.2F1111
   oo.211
   ooo1
   oo.211
   oo.3F2 11
   ooooF2 1F
```

A regra local não prova nada. A enumeração **sem** o total de minas também não prova nada. Com
o total, 22 casas caem de uma vez: sobraram só **2** distribuições de minas de pé, e nelas as
22 casas marcadas com `o` estão vazias nas duas.

### 6. Chutar melhor vale quase tanto quanto deduzir melhor

`ENUMERACAO` e `ENUMERACAO_E_CHUTE_BOM` provam exatamente a mesma coisa: as duas terminam 16
partidas de especialista sem precisar chutar. A única diferença é em qual casa a moeda é
jogada quando não há saída, e isso sozinho leva de **35 para 51** vitórias.

Repare na linha de especialista: o jogador com chute bom chuta **mais** vezes (270 contra 210)
e explode **menos** (49 contra 65). Ele chuta mais porque sobrevive mais tempo.

## O juiz independente

Toda conta da `Enumeracao` passa pela mesma cabeça: separar em pedaços, podar, somar
polinômios. Se o raciocínio estiver torto, ela erra com convicção. Por isso existe a
`ForcaBruta`, que não sabe o que é fronteira nem pedaço: ela monta todo campo possível, e para
cada um confere **todas** as casas abertas, uma a uma.

```
tabuleiro  partidas  posicoes  comparadas  casas  discordancias  maior varredura
6x6/6           200       451         435   4544              0           2^22
7x7/8           200       779         491   5413              0           2^22
8x8/10          200       676         406   4278              0           2^22
```

**14.235 casas comparadas, zero discordâncias.** Não só a conclusão ("é mina", "é livre"): a
contagem inteira de cada uma, cruzada em multiplicação, sem passar por ponto flutuante.

## O que as medidas me corrigiram

**Eu tinha certeza de que a regra do total de minas ia render partidas.** É a primeira coisa
que qualquer jogador faz: o contador chegou a zero, abre tudo. Botei no código, medi, e deu 323
contra 323 no iniciante, 93 contra 93 no intermediário, 3 contra 3 no especialista. Zero.
Raciocinando depois de medir: a metade "é tudo mina" dessa regra só diz o que as contas locais
já diziam, e a metade "acabou" só dispara quando a partida já estava ganha. O total só vira
dedução de verdade quando entra na **contagem**, e aí ele responde por quase metade do que a
enumeração prova a mais. Foi a medida mais surpreendente do repositório, e as duas pontas dela
estão na mesma tabela.

**Escrevi três exemplos de tabuleiro na mão e errei os três.** Em todos, a vizinhança que eu
imaginei não era a vizinhança de verdade, porque no campo minado uma casa vê as oito em volta,
inclusive as quatro diagonais, e a conta da ponta e a do canto são diferentes da conta do meio.
Os testes pegaram os três antes de qualquer coisa ir para o README. Hoje eu não escrevo
posição de campo minado na mão sem rodar.

**As posições que eu mostro aqui eu não inventei: achei jogando.** A primeira versão do `Casos`
procurava "a regra local travou e a enumeração resolveu" e "só o total resolve", e devolvia a
**mesma** posição para as duas, porque a primeira posição em que a regra local trava também era
uma em que só o total resolvia. Mostrar o mesmo tabuleiro duas vezes com legendas diferentes
teria passado batido em qualquer revisão. A correção foi afiar a pergunta: o primeiro caso
agora exige que a enumeração resolva **sem** precisar do total.

**Desconfiei de um número certo.** A coluna "destravadas só pelo total" deu 7 nos três níveis.
Três valores iguais numa tabela quase sempre é variável que não foi zerada, então fui conferir
com outros tamanhos de amostra: 2, 3 e 2 com amostras menores. Era coincidência mesmo. Não
troquei os tamanhos para esconder a cara estranha da tabela; está escrito no texto do achado 2.

**A força bruta não achou um único erro, e é a parte em que eu mais confio.** Ela não serviu
para consertar nada: serviu para que o "zero discordâncias" seja uma medida e não uma opinião.
Sem ela, a frase "a enumeração está certa" valeria exatamente o que vale a minha confiança no
meu próprio raciocínio sobre polinômios e binomiais, que é pouco.

## Como está organizado

```
src/main/java/br/com/conde/minas/
  Campo.java        onde as minas estao; sorteio com a primeira casa limpa
  Visao.java        o que se ve; restricoes, fronteira e escuro
  Jogo.java         abrir em cascata, marcar, ganhar, explodir
  Deducao.java      o que foi provado: minas e livres
  RegraLocal.java   as duas regras de olhar um numero por vez
  Enumeracao.java   o juiz completo, em contagem inteira
  ForcaBruta.java   o juiz burro, que confere a tela inteira campo por campo
  Jogador.java      joga a partida sozinha, nos quatro jeitos
  Casos.java        procura e mostra posicoes de verdade
  Medidor.java      as tabelas
src/test/java/br/com/conde/minas/
  CampoTest, VisaoTest, JogoTest, RegraLocalTest, EnumeracaoTest,
  ForcaBrutaTest, JogadorTest, MedidasTest
```

53 testes. O `MedidasTest` prende cada número deste README a uma medida, inclusive as três
posições mostradas, que são conferidas contra o campo de verdade antes de serem impressas.

## Licença

MIT.
