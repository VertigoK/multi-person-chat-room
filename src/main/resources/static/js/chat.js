$(function() {
    let FADE_TIME = 150; //ms
    let COLORS = [
        '#e21400', '#91580f', '#f8a700', '#f78b00',
        '#58dc00', '#287b00', '#a8f07a', '#4ae8c4',
        '#3b88eb', '#3824aa', '#a700ff', '#d300e7'
    ];

    //Initialize variables.
    let $window = $(window);
    let $usernameInput = $('.usernameInput'); //username
    let $messages = $('.messages'); //chat area
    let $inputMessage = $('.inputMessage'); //message box
    let $loginPage = $('.login.page'); //login page
    let $chatPage = $('.chat.page'); //chat room page

    //Create WebSocket connection.
    //The port number should match the one given in application.yml.
    let ws = new WebSocket('ws://localhost:9090');
    let connected = false; //default connection status

    //Parameters for setting username
    let username;
    let $currentInput = $usernameInput.focus();

    //Keyboard events
    $window.keydown(function(event) {
        if (!(event.ctrlKey || event.metaKey || event.altKey)) {
            $currentInput.focus();
        }
        //Listen for the enter key
        if (event.which === 13) {
            if (username) {
                sendMessage();
            } else {
                setUsername();
            }
        }
    });

    //Set username.
    function setUsername() {
        username = cleanInput($usernameInput.val().trim());
        if (username) {
            $loginPage.fadeOut();
            $chatPage.show();
            $loginPage.off('click');
            $currentInput = $inputMessage.focus();
            //Send a message to the server when a new user enters the chat room.
            let msg = {};
            msg.t = 1;  //message type 1 (welcome)
            msg.n = username;   //username
            msg.room_id = 1;    //room id
            ws.send(JSON.stringify(msg));
        }
    }

    //Send a chat message to the server.
    function sendMessage() {
        if (connected) {
            let msg = {};
            msg.t = 2;  //message type 2 (chat)
            msg.n = username;   //username
            msg.body = cleanInput($inputMessage.val()); //message body
            ws.send(JSON.stringify(msg));
            addChatMessage({username:username, message:msg.body});
            $inputMessage.val("");
        } else {
            log("Disconnected from server. Refresh to reconnect.");
        }
    }

    // document.addEventListener('visibilitychange', function() {
    //     if (document.visibilityState === 'hidden') {
    //         let url = "http://localhost:8080/chat";
    //         let msg = {};
    //         msg.t = 2;
    //         msg.n = username;
    //         navigator.sendBeacon(url, msg.n);
    //     }
    // });

    //Clear the injected information in the input box
    function cleanInput(input) {
        return $('<div/>').text(input).html();
    }

    //Output a chat message.
    function addChatMessage(data, options) {
        options = options || {};
        let $usernameDiv = $('<span class="username"/>')
                            .text(data.username)
                            .css('color', getUsernameColor(data.username));
        let $messageBodyDiv = $('<span class="messageBody">')
                            .text(data.message);
        let typingClass = data.typing ? 'typing' : '';
        let $messageDiv = $('<li class="message"/>')
                            .data('username', data.username)
                            .addClass(typingClass)
                            .append($usernameDiv, $messageBodyDiv);
        addMessageElement($messageDiv, options);
    }

    //Output log information.
    function log(message, options) {
        let $el = $('<li>').addClass('log').text(message);
        addMessageElement($el, options);
    }

    //DOM operation
    function addMessageElement(el, options) {
        let $el = $(el);
        if (!options) {
            options = {};
        }
        if (typeof options.fade === 'undefined') {
            options.fade = true;
        }
        if (typeof options.prepend === 'undefined') {
            options.prepend = false;
        }
        if (options.fade) {
            $el.hide().fadeIn(FADE_TIME);
        }
        if (options.prepend) {
            $messages.prepend($el);
        } else {
            $messages.append($el);
        }
        $messages[0].scrollTop = $messages[0].scrollHeight;
    }

    //Given colors, return one based off username through a hash function to color it.
    function getUsernameColor(username) {

        let hash = 7;
        for (let i = 0; i < username.length; i++) {
            hash = username.charCodeAt(i) + (hash << 5) - hash;
        }
        //Calculate color index
        let index = Math.abs(hash % COLORS.length);
        return COLORS[index];
    }

    //Get focus.
    $loginPage.click(function () {
        $currentInput.focus();
    });

    //Update the connection state when the connection is established.
    ws.onopen = function (e) {
        connected = true;
        console.log('Connection to server opened');
    }

    //Process messages sent by the server
    ws.onmessage = function(e) {
        let msg = JSON.parse(e.data);
        console.log(msg);
        switch (msg.t) {
            case 0:
                //connection establishment response
                break;
            case -1:
                //Received a response to entering the chat room with room information
                log(username + "님, 대화방에 오신 걸 환영합니다!");
                break;
            case -2:
                //Receive messages from others
                let data = {
                    username: msg.n,
                    message: msg.body
                };
                addChatMessage(data);
                break;
            case -10001:
                //Receive a message that someone else has entered the chat room
                log(msg.n + "님이 입장했습니다.");
                break;
            case -11000:
                //Receive a message that someone else has left the chat room
                log(msg.n + "님이 대화방을 나갔습니다.");
                break;
        }
    }

    //Update the connection state when the connection is dead
    ws.onclose = function(e) {
        connected = false;
        console.log('Connection to server closed');
    }

    ws.onerror = function(e) {
        connected = false;
    }
});
