<script>
 export const name = "Kibitzer";

 var ws = new WebSocket(getUrl(), "was-ws");
 var sessionId = "";
 var turnId = "";
 var command = "";
 var nic = "";
 var nicTurn = "";
 var messages = "";
 var joined = false;
 var input = null;
 var cardSpread = 50;
 var dragSource = 0;
 var kDragHand = 1;
 var kDragPile = 2;
 var kDragDeck = 3;
 var pileHover = false;
 var handHover = false;
 var faceDown = false;
 var pile = [];
 var hand = [];
 var players = [];
 var game = "";
 var confirm = null;
 var turnTimerId = null;
 var turnTimeout = 30;

 $: titleText = name
              + (game ? ' [' + game + '] ' : "")
              + (nic ? ' - Nic: (' + nic + ')' : '')
              + (nicTurn ? ' Turn: (' + nicTurn + ')' : '');

 function getHand(array) {
   var result = [];
   for (var i = 0; i < array.length; i++) {
     result.push({face: array[i], selected: false});
   }
   return result;
 }

 function getSelected() {
   let result = [];
   for (var card of hand) {
     if (card.selected) {
       result.push(card.face);
     }
   }
   return JSON.stringify(result);
 }

 function showHelp() {
   messages += "<h2>Help</h2>";
   messages += "<p><i>clear</i> - clear messages.";
   messages += "<p><i>deal</i> - start the game.";
   messages += "<p><i>help</i> - print this summary.";
   messages += "<p><i>nic &lt;text&gt;</i> - set your nickname.";
   messages += "<p><i>room [#]</i> - enter room.";
   messages += "<p><i>&lt;text&gt;</i> - send a chat message.";
 }

 function showTurn() {
   window.document.title = name + " @";
 }

 function onMessage(json) {
   switch (json.id) {
     case "init":
       sessionId = json.data.sessionId;
       players = json.data.players;
       messages += "<p>" + json.data.welcome;
       messages += "<h2>Welcome to Kibitzer</h2>";
       messages += "<p>Click an available avatar on the right to join the game."
       showHelp();
       break;
     case "players":
       messages += "<p>" + json.data.message;
       players = json.data.players;
       turnId = json.data.turn;
       var player = players.find(e => e.sessionId == sessionId);
       if (player !== undefined) {
         joined = true;
         nic = player.nic;
       }
       player = players.find(e => e.sessionId == turnId && e.active);
       if (player !== undefined) {
         // when the nic changes
         nicTurn = player.nic;
       } else if (turnId == -1) {
         nicTurn = "";
       }
       if (sessionId == json.data.dealId) {
         ws.send("deal:");
       }
       if (sessionId == json.data.clearHandId) {
         hand = [];
       }
       if (json.data.game) {
         game = json.data.game;
       }
       break;
     case "cards":
       messages += "<p>" + json.data.message;
       pile = getHand(json.data.pile);
       turnId = json.data.turn;
       faceDown = json.data.faceDown;
       game = json.data.game;
       var player = players.find(e => e.sessionId == turnId);
       if (player !== undefined) {
         // when the turn changes
         nicTurn = player.nic;
       }
       if (json.data.hand && sessionId == json.data.sessionId) {
         hand = getHand(json.data.hand);
       }
       if (sessionId == turnId) {
         if (turnTimerId) {
           window.clearInterval(turnTimerId);
         }
         var count = 0;
         showTurn();
         turnTimerId = window.setInterval(function() {
           window.document.title = name;
           if (sessionId !== turnId) {
             window.clearInterval(turnTimerId);
             turnTimerId = null;
           } else if (++count >= turnTimeout) {
             ws.send("skip:");
             window.clearInterval(turnTimerId);
             turnTimerId = null;
           } else if (count % 2 == 0) {
             showTurn();
           }
         }, 2000);
       }
       break;
     case "exchange":
       messages += "<p>" + json.data.message;
       if (sessionId != json.data.fromId) {
         // setup dialog
         confirm = {
           title: "Take " + json.data.hand + " from " + json.data.from + " ?",
           accept() {
             ws.send("exch:Y:" + json.data.fromId + ":" + JSON.stringify(json.data.hand));
             confirm = null;
           },
           cancel() {
             confirm = null;
           }
         }
       }
       break;
     case "message":
       messages += "<p>" + json.data;
       break;
     case "nic":
       break;
     case "shuffle":
       ws.send("deal:");
       break;
     case "exit":
       messages += "<p>" + json.data.name + " has left the game";
       players = json.data.players;
       break;
     default:
       console.log("unknown message:");
       console.log(json);
       break;
   }
 }

 document.addEventListener("DOMContentLoaded", function() {
   try {
     ws.onopen = function() {
       ws.send("init:")
     };
     ws.onmessage = function(msg) {
       try {
         onMessage(JSON.parse(msg.data));
       } catch (e) {
         console.log(msg.data);
         console.log("Error: " + e);
       }
     };
     ws.onclose = function() {
       console.log("closed");
     };
   } catch (exception) {
     console.log(exception);
   }
 });

 function getUrl() {
	 var pcol;
	 var u = document.URL;
	 if (u.substring(0, 5) === "https") {
		 pcol = "wss://";
		 u = u.substr(8);
	 } else {
		 pcol = "ws://";
		 if (u.substring(0, 4) === "http") {
			 u = u.substr(7);
     }
	 }
	 return pcol + u.split(":")[0] + ":7681";
 }

 function broadcast(message) {
   ws.send("chat:" + message);
 }

 function dragOverDeck(e) {
   if (dragSource == kDragPile && !pile.length) {
     // drag the empty pile onto the deck to start
     e.preventDefault();
   }
   pileHover = false;
   handHover = false;
 }

 function dragOverHand(e) {
   if (dragSource == kDragDeck) {
     // pickup from deck
     e.preventDefault();
     handHover = true;
   }
   pileHover = false;
 }

 function dragOverPile(e) {
   if (dragSource == kDragHand) {
     // put down selected hand cards onto the pile
     e.preventDefault();
     pileHover = true;
   }
   handHover = false;
 }

 function dragOverPlayer(e, player) {
   if (dragSource == kDragHand && player.sessionId != sessionId && player.active) {
     // give selected hand cards to another player
     e.preventDefault();
   }
   pileHover = false;
   handHover = false;
 }

 function dropOnDeck(e) {
   e.preventDefault();
   dragEnd();
   broadcast("drop onto deck");
 }

 function dropOnHand(e) {
   e.preventDefault();
   dragEnd();
   ws.send("picu:1");
 }

 function dropOnPile(e) {
   e.preventDefault();
   dragEnd();
   ws.send("putd:" + getSelected());
 }

 function dropOnPlayer(e, player) {
   e.preventDefault();
   dragEnd();
   if (player.sessionId != sessionId && player.active) {
     ws.send("exch:Q:" + player.sessionId + ":" + getSelected());
   }
 }

 function dragEnd() {
   dragSource = 0;
   pileHover = false;
   handHover = false;
 }

 function dragStart(e, source) {
   dragSource = source;
   e.dataTransfer.dropEffect = "link";
 }

 function deal() {
   messages += "<p>Deal";
   ws.send("shuf:");
 }

 function enterText() {
   var args = command.split(" ");
   switch (args[0]) {
     case "help":
       showHelp();
       break;
     case "":
       break;
     case "clear":
       messages = "";
       break;
     case "deal":
       deal();
       break;
     case "nic":
       if (sessionId && args[1]) {
         ws.send("nicn:" + command.substring(4));
       }
       break;
     case "room":
       messages += "<p>" + command;
       ws.send("room:" + (args[1] ? command.substring(5) : ""));
       break;
     default:
       if (sessionId) {
         broadcast(command);
       } else {
         messages += "<p>enter 'help'</p>";
       }
       break;
   }
   command = "";
   input.focus();
 }

 function init(el) {
   input = el;
   input.focus();
 }

 function selectPlayer(slot) {
   ws.send("join:" + slot);
   input.focus();
 }

</script>

<main>
  <div class="main">
    <h2>{titleText}</h2>
    {#if confirm}
      <div class="confirm">
        {confirm.title}
        <span>
          <button type="text" title="Take cards" on:click="{() => confirm.accept()}">
            &#x1F44D;
          </button>
          <button type="text" title="Reject cards" on:click="{() => confirm.cancel()}">
            &#x1F44E;
          </button>
        </span>
      </div>
    {/if}
    {#if hand.length == 0 && joined}
      <div class="deal">
        <button type="text" title="Deal cards" on:click="{() => deal()}">
          &#129330;
        </button>
      </div>
    {/if}
    <div class="game">
      <div class="{pileHover ? 'heartbeat' : ''}"
           on:drop="{dropOnPile}"
           on:dragover="{dragOverPile}">
        {#if pile.length > 0}
          <img src="./images/deck/{pile[pile.length - 1].face}.png"
               alt="card"
               title="{pile[pile.length - 1].face} over {pile.length} cards"
               draggable="true"
               on:dragend="{dragEnd}"
               on:dragstart="{(e) => dragStart(e, kDragPile)}" />
        {:else}
          <a href="https://en.wikipedia.org/wiki/Category:Card_games_by_number_of_players"
             title="Card games by number of players" target="_blank">
            <img alt="Card_back_12" src="./images/Card_back_12.png"/>
          </a>
        {/if}
      </div>
      <div class="deck">
        <img src="./images/Card_back_01.png"
             alt="Card_back_01"
             class="{turnId == sessionId ? "pick" : ""}"
             draggable="true"
             on:drop="{dropOnDeck}"
             on:dragover="{dragOverDeck}"
             on:dragend="{dragEnd}"
             on:dragstart="{(e) => dragStart(e, kDragDeck)}" />
      </div>
    </div>
    <div class="hand">
      <div class="{handHover ? 'cards heartbeat' : 'cards'}"
           on:drop="{dropOnHand}"
           on:dragover="{dragOverHand}">
        {#each hand as card, id}
          <div id="hand_{id}"
               style="z-index: {id}; grid-column: {(1 + (id * 3))} / {(cardSpread + ((id + 1) * 3))};"
               class="{card.selected ? "selected" : ""}">
            <img src="{faceDown ? './images/Card_back_01' : './images/deck/' + card.face}.png"
                 alt="{faceDown ? 'faceDown' : card.face}"
                 draggable="{card.selected}"
                 on:click="{(e) => {card.selected = !card.selected}}"
                 on:dragend="{dragEnd}"
                 on:dragstart="{(e) => dragStart(e, kDragHand)}" />
          </div>
        {/each}
      </div>
    </div>
  </div>
  <div class="chat">
    <div class="stream">
      <span contenteditable="false" bind:innerHTML={messages}>
      </span>
    </div>
    <div class="input">
      <div class="input-box">
        <input type="text"
               placeholder="Enter command"
               title="Enter command"
               maxlength="100"
               use:init
               on:keypress="{(e) => {if (e.charCode == 13) enterText()}}"
               bind:value="{command}">
      </div>
      <div>
        <button type="text" on:click="{() => enterText()}">
          &gtcir; Send
        </button>
      </div>
    </div>
  </div>
  <div class="players">
    {#each players as player, i}
      <button on:click="{() => selectPlayer(i)}" title="{(joined || player.active) ? player.nic : 'click to join' }">
        <img id="player{i + 1}" alt="player_{i + 1}"
             on:drop="{(e) => dropOnPlayer(e, player)}"
             on:dragover="{(e) => dragOverPlayer(e, player)}"
             on:dragend="{dragEnd}"
             class="{(player.sessionId == sessionId ? 'me' : !player.active ? 'inactive' : 'other')
                    + (player.sessionId == turnId ? ' turn' : '')}"
             style="height: {100/players.length}%;" src="./images/avatars/player{i + 1}.png"/>
      </button>
    {/each}
  </div>
</main>

<style>
 main {
   padding: 0px;
   margin: 0 auto;
   display: flex;
   flex-direction: row;
   font-size: 17px;
 }

 @media (min-width: 640px) {
   main {
     max-width: none;
   }
 }

 .heartbeat {
   animation: heartbeat 1.3s ease-in-out;
 }

 img {
   height: 16.5em;
 }

 .back {
   padding-left: 5px;
 }

 .players {
   max-width: 100px;
   height: 100%;
   flex: 0 0 20%;
   padding-top: 1em;
 }

 .players button {
   display: contents;
 }

 .players img {
   cursor: pointer;
 }

 .players img.turn {
   animation: heartbeat 1.3s ease-in-out infinite both;
 }

 .players img.inactive {
   filter: opacity(0.1);
 }

 .players img.inactive:hover {
   filter: opacity(0.4);
 }

 .players img.other {
   filter: opacity(0.5);
 }

 .players img.other:hover {
   filter: opacity(0.7);
 }

 .players img.me {
   filter: opacity(1);
 }

 .players img.me:hover {
   transform: scale(1.1);
 }

 .main {
   flex: 1;
   min-height: 100vh;
   padding-left: 2em;
 }

 .main h2 {
   height: 33px;
   margin: 0 auto;
   padding: 2px;
   text-align: center;
 }

 .confirm {
   position: absolute;
   left: 35%;
 }

 .confirm button {
   cursor: pointer;
   background-color: transparent;
   border: none;
   color: white;
 }

 .deal {
   position: absolute;
   top: 1.1em;
   left: 38%;
 }

 .deal button {
   cursor: pointer;
   font-size: 2em;
   color: #dd0;
   background-color: transparent;
   border: none;
 }

 .game {
   height: calc(50% - 33px);
   justify-content: center;
   align-items: center;
   display: flex;
 }

 .deck .pick {
   cursor: grab;
 }

 .hand {
   height: 50%;
   position: relative;
   width: 100%;
   justify-content: center;
   align-items: center;
 }

 .hand > .cards {
   display: grid;
 }

 .hand > .cards > div {
   margin: 1em 0px 0px 1em;
   grid-row-start: 1;
 }

 .hand > .cards > div.selected {
   transform: translate(0px, -1.1em);
 }

 .hand > .cards > div > img {
   cursor: pointer;
 }

 .hand > .cards > div.selected > img {
   cursor: grab;
 }

 .chat {
   flex: 0 0 17%;
   display: flex;
   flex-direction: column;
   background-color: #00bb88;
   height: 100vh;
   max-height: 100vh;
   border: 5px;
 }

 .chat .stream {
   margin: auto;
   flex: 1 1 auto;
   flex-direction: column-reverse;
   display: flex;
   align-items: flex-end;
   overflow: auto;
   overflow: overlay;
   height: 100%;
   width: 100%;
 }

 .chat .stream > span {
   padding: 10px;
   width: 16vw;
 }

 .chat .input {
   display: flex;
   background-color: #fff;
   padding: 10px;
 }

 .chat .input-box {
   flex-basis: 100%;
   padding-right: 10px;
 }

 .chat .input input {
   padding: 3px 5px 3px 5px;
   width: 100%;
   box-sizing: border-box;
   font-weight: bold;
 }

 .chat .input button {
   white-space: nowrap;
   outline: 0;
   cursor: pointer;
   word-break: break-word;
   box-shadow: 0 0 0 1px #3078af;
   background: #fff;
   color: #3078af;
   font-weight: 700;
   padding: 3px 10px;
   border-radius: 12px;
   border: 1px solid #fff;
 }

 .chat p {
   margin-top: 0;
   text-indent: 2em;
   padding: 0 5px 0 5px;
 }

 @keyframes heartbeat {
   0% {
     transform: scale(1);
   }
   14% {
     transform: scale(1.03);
   }
   28% {
     transform: scale(1);
   }
   42% {
     transform: scale(1.02);
   }
   70% {
     transform: scale(1);
   }
 }

</style>
