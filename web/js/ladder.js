var Ladder = (function() {
    "use strict";

    var STATUS_BUTTON_HTML_PREFIX = '<a v-on:click="changeStatus()"' +
    'class="btn btn-raised btn-xs toggle-button ';
    var STATUS_BUTTON_HTML_SUFFIX = ' ">{{ status }}</a>';

    var EDIT_BUTTON_HTML = '<a v-on:click="editPair()" class="edit-button btn btn-info btn-fab btn-fab-mini"><i class="material-icons md-light">create</i></a>';

    var showModal = function(index) {
        var modalId = "#modal" + index;
        $(modalId).modal("show");
    };

    var hideModal = function(index) {
        var modalId = "#modal" + index;
        $(modalId).modal("hide");
    };

    function Ladder(ladderData) {

        var playingButton;
        var notPlayingButton;

        playingButton = Vue.extend({
            data: function() {
                return { status: "Playing" };
            },
            props: ['index'],
            template: STATUS_BUTTON_HTML_PREFIX + 'btn-success' + STATUS_BUTTON_HTML_SUFFIX,
            methods: {
                changeStatus: function() {
                    this.$parent.changeStatus(this.index);
                }
            }
        });
        Vue.component('playing-button', playingButton);

        notPlayingButton = Vue.extend({
            data: function() {
                return { status: "Not Playing" };
            },
            props: ['index'],
            template: STATUS_BUTTON_HTML_PREFIX + 'btn-danger' + STATUS_BUTTON_HTML_SUFFIX,
            methods: {
                changeStatus: function(index) {
                    this.$parent.changeStatus(this.index);
                }
            }
        });
        Vue.component('not-playing-button', notPlayingButton);

        var editButton = Vue.extend({
            props: ['index'],
            template: EDIT_BUTTON_HTML,
            methods: {
                editPair: function() {
                    showModal(this.index);
                }
            }
        });
        Vue.component('edit-button', editButton);

        this.component = new Vue({
            el: '#ladder',
            data: {
                ladder: ladderData,
                newPairData: {
                    player1: {
                        firstName: "",
                        lastName: "",
                        phoneNumber: ""
                    },
                    player2: {
                        firstName: "",
                        lastName: "",
                        phoneNumber: ""
                    },
                    position: ""
                },
                mode: 'read'
            },
            components: {
                playing: playingButton,
                notplaying: notPlayingButton,
                edit: editButton
            },
            methods: {
                changeStatus: this.changeStatus,
                changeMode: this.changeMode,
                onDelete: this.deletePair,
                onAdd: this.addPair,
                refreshLadder: this.refreshLadder,
                refreshMode: this.refreshMode
            }
        });
    }

    Ladder.prototype.changeStatus = function(index) {
        var api = new API();
        var pair = this.ladder[index];
        if (pair.playingStatus === "playing") {
            api.updatePairStatus(pair.id, "not playing", function(response) {
                pair.isPlaying = false;
                pair.playingStatus = "notplaying";
            });
        }
        else {
            api.updatePairStatus(pair.id, "playing", function(response) {
                pair.isPlaying = true;
                pair.playingStatus = "playing";
            });
        }
    };

    Ladder.prototype.changeMode = function() {
        if (this.mode === 'read') {
            this.mode = 'edit';
            this.ladder.forEach(function(entry) {
                entry.playingStatus = 'edit';
            });
        }
        else {
            this.mode = 'read';
            this.ladder.forEach(function(entry) {
                entry.playingStatus = entry.isPlaying ? 'playing' : 'notplaying';
            });
        }
    };

    Ladder.prototype.deletePair = function(index) {
        var answer = confirm("Are you sure you want to delete this pair?");
        if (answer) {
            var api = new API();
            api.removePair(this.ladder[index].id, this.refreshLadder);
            hideModal(index);
        }
    };

    Ladder.prototype.addPair = function(event) {
        var api = new API();

        var player1Data = this.newPairData.player1;
        var player1 = api.prepareNewPlayer(player1Data.firstName,
            player1Data.lastName, player1Data.phoneNumber);

        var player2Data = this.newPairData.player2;
        var player2 = api.prepareNewPlayer(player2Data.firstName,
            player2Data.lastName, player2Data.phoneNumber);

        var ladderPosition = this.newPairData.position;
        if (ladderPosition === "") {
            ladderPosition = -1;
        }

        api.addPair([player1, player2], ladderPosition, this.refreshLadder);
        $("#addPairModal").modal("hide");
    };

    Ladder.prototype.refreshLadder = function() {
        var api = new API();
        api.getLadder(function(ladderData) {
            this.ladder = ladderData;
            this.refreshMode();
        }.bind(this));
    };

    Ladder.prototype.refreshMode = function() {
        if (this.mode === 'edit') {
            this.ladder.forEach(function(entry) {
                entry.playingStatus = 'edit';
            });
        }
        else {
            this.ladder.forEach(function(entry) {
                entry.playingStatus = entry.isPlaying ? 'playing' : 'notplaying';
            });
        }
    }

    return Ladder;
})();
