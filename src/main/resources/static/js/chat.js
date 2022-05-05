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

    //Create a WebSocket connection.
    //The port number should match the one given in application.yml.
    let ws = new WebSocket('ws://localhost:9090');
    let connected = false; //default connection state

    //Parameters for setting username
    let username;
    let $currentInput = $usernameInput.focus();

    //Keyboard events (1)
    window.addEventListener('keydown', (event) => {
        if (!(event.ctrlKey || event.metaKey || event.altKey)) {
            $currentInput.focus();
        }
        //Listen to the 'Enter' key
        if (event.key === 'Enter') {
            if (username) {
                sendMessage();
            } else {
                setUsername();
            }
        }
    });

    //Keyboard events (2)
    // $window.keydown(function(event) {
    //     if (!(event.ctrlKey || event.metaKey || event.altKey)) {
    //         $currentInput.focus();
    //     }
    //     //Listen to the 'Enter' key
    //     if (event.which === 13) {
    //         if (username) {
    //             sendMessage();
    //         } else {
    //             setUsername();
    //         }
    //     }
    // });

    //When closing a tab or browser
    window.onbeforeunload = function () {
        let msg = {};
        msg.t = 3;
        msg.n = username;
        msg.roomId = 1;
        ws.send(JSON.stringify(msg));
        // return true;
    }

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
            msg.roomId = 1;    //room id
            ws.send(JSON.stringify(msg));
        }
    }

    //Send a chat message to the server.
    function sendMessage() {
        if (connected) {
            let msg = {};
            msg.t = 2;  //message type 2 (chat)
            msg.n = username;   //username
            msg.roomId = 1;    //room id
            msg.body = cleanInput($inputMessage.val()); //message body
            //output a message to other users.
            ws.send(JSON.stringify(msg));
            //output a message to my own window.
            addChatMessage({username: username, message: msg.body});
            $inputMessage.val("");
        } else {
            outputLog("Disconnected from server; refresh to reconnect.");
        }
    }

    //Clear the injected information in the input box
    function cleanInput(input) {
        return $('<div/>').text(input).html();
    }

    //Output a chat message to my own window.
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

    //Output log information to the client.
    function outputLog(message, options) {
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

    //Process messages sent by the server. Invoked when data is received through a WebSocket.
    ws.onmessage = function(e) {
        let msg = JSON.parse(e.data);
        console.log(msg);
        switch (msg.t) {
            case 0:     //connection establishment response
                break;
            case -1:    //Receive a welcome message when entering the chat room.
                outputLog(username + ", welcome to the chat room!");
                break;
            case -2:    //Receive messages from others
                let data = {
                    username: msg.n,
                    message: msg.body
                };
                addChatMessage(data);
                break;
            case -10001:    //Receive a message that someone else has entered the chat room.
                outputLog(msg.n + " just entered the room.");
                break;
            case -11000:    //Receive a message that someone else has left the chat room.
                outputLog(msg.n + " left the room.");
                break;
        }
    }

    //Update the connection state when the connection with a websocket is closed.
    ws.onclose = function(e) {
        connected = false;
        console.log('Connection to server closed');
    }

    //Update the connection state when an error occurs.
    ws.onerror = function(e) {
        connected = false;
        console.log('Error occurred')
    }
});
