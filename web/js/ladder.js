var Ladder = (function () {
    "use strict";

    var NUM_ENTRIES_PER_PAGE = 20;

    var STATUS_BUTTON_HTML_PREFIX = '<a v-on:click="changeStatus()"' +
        'class="btn btn-raised btn-xs toggle-button ';
    var TIME_SELECT_HTML = '<button class="btn-link" v-on:click="timeSelection()" id="timeSelect">preferred time</button>';
    var STATUS_BUTTON_HTML_SUFFIX = ' ">{{ status }}</a>';

    var EDIT_BUTTON_HTML = '<a v-on:click="editPair()" class="edit-button btn btn-info btn-fab btn-fab-mini"><i class="material-icons md-light">create</i></a>';
    var PAGE_BUTTON_PREFIX = '<a v-on:click="changeCurrentPage()"> Page {{ ';
    var PAGE_BUTTON_SUFFIX = ' }} </a>';

    var showModal = function (index) {
        var modalId = "#modal" + index;
        $(modalId).modal("show");
    };

    var hideModal = function (index) {
        var modalId = "#modal" + index;
        $(modalId).modal("hide");
    };

    function Ladder(ladderData) {


        var playingButton;
        var notPlayingButton;
        var pageButton;
        var ladderPages = [];
        var currentPage = [];

        playingButton = Vue.extend({
            data: function () {
                return {status: "Playing"};
            },
            props: ['index'],
            template: STATUS_BUTTON_HTML_PREFIX + 'btn-success' + STATUS_BUTTON_HTML_SUFFIX + TIME_SELECT_HTML,
            methods: {
                changeStatus: function () {
                    this.$parent.changeStatus(this.index);
                },

                timeSelection: function () {
                     var timeModal = document.getElementById("timeSelectModal" + this.index);
                     var closeBtn = document.getElementById("timeCloseBtn" + this.index);
                     var saveBtn = document.getElementById("timeSaveButton" + this.index);
                     var firstCheckBox = document.getElementById("checkbox8pm" + this.index);
                     var secondCheckBox = document.getElementById("checkbox9pm" + this.index);

                    timeModal.style.display = "block";
                    var isFirstCheckBoxChecked;
                    Boolean(isFirstCheckBoxChecked);
                    if (firstCheckBox.checked) {
                        isFirstCheckBoxChecked = true;
                    }
                    else { isFirstCheckBoxChecked = false; }

                     closeBtn.onclick = myCloseFunction;
                     saveBtn.onclick = function() {
                         timeModal.style.display = "none";
                     };
                     window.onclick = function(event) {
                         if (event.target == timeModal) {
                             timeModal.style.display = "none";
                             if (isFirstCheckBoxChecked) {
                                 firstCheckBox.checked = true;
                                 secondCheckBox.checked = false;
                             }
                         }
                     };
                     firstCheckBox.onclick = function() {
                         secondCheckBox.checked = false;
                     };
                    secondCheckBox.onclick = function() {
                        firstCheckBox.checked = false;
                    };

                    function myCloseFunction() {
                        timeModal.style.display = "none";
                        if (isFirstCheckBoxChecked) {
                            firstCheckBox.checked = true;
                            secondCheckBox.checked = false;
                        }
                    }
                }
            }
        });
        Vue.component('playing-button', playingButton);

        notPlayingButton = Vue.extend({
            data: function () {
                return {status: "Not Playing"};
            },
            props: ['index'],
            template: STATUS_BUTTON_HTML_PREFIX + 'btn-danger' + STATUS_BUTTON_HTML_SUFFIX,
            methods: {
                changeStatus: function (index) {
                    this.$parent.changeStatus(this.index);
                }
            }
        });
        Vue.component('not-playing-button', notPlayingButton);

        pageButton = Vue.extend({
            props: ['index'],
            template: PAGE_BUTTON_PREFIX + 'index' + PAGE_BUTTON_SUFFIX,
            methods: {
                changeCurrentPage: function (index) {
                    this.$parent.changeCurrentPage(this.index - 1);
                }
            }
        });
        Vue.component('page-button', pageButton);

        var editButton = Vue.extend({
            props: ['index'],
            template: EDIT_BUTTON_HTML,
            methods: {
                editPair: function () {
                    showModal(this.index);
                }
            }
        });

        var onValid = function (elementId) {
            $(elementId).prop("disabled", false);
        };

        var onInvalid = function (elementId) {
            $(elementId).prop("disabled", true);
        };

        this.component = new Vue({
            el: '#ladder',
            data: {
                ladder: ladderData,
                ladderPages: ladderPages,
                currentPage: currentPage,
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
                changeCurrentPage: this.changeCurrentPage,
                onDelete: this.deletePair,
                onAdd: this.addPair,
                onUpdate: this.updatePair,
                refreshLadder: this.refreshLadder,
                refreshMode: this.refreshMode,
                updateLadder: this.updateLadder,
                onValid: onValid,
                onInvalid: onInvalid,
                setTime: this.setTime
            }
        });
    }

    Ladder.prototype.changeStatus = function (index) {
        var api = new API();
        var pair = this.ladder[index];
        if (pair.playingStatus === "playing") {
            api.updatePairStatus(pair.id, "not playing", function (response) {
                pair.isPlaying = false;
                pair.playingStatus = "notplaying";
                document.getElementById("checkbox8pm" + index).checked = true;
                document.getElementById("checkbox9pm" + index).checked = false;;
            });
        }
        else {
            api.updatePairStatus(pair.id, "playing", function (response) {
                pair.isPlaying = true;
                pair.playingStatus = "playing";
            });
        }
    };

    Ladder.prototype.changeCurrentPage = function (index) {
        this.currentPage = this.ladderPages[index];
    };

    Ladder.prototype.changeMode = function () {
        if (this.mode === 'read') {
            this.mode = 'edit';
            this.ladder.forEach(function (entry) {
                entry.playingStatus = 'edit';
            });
        }
        else {
            this.mode = 'read';
            this.ladder.forEach(function (entry) {
                entry.playingStatus = entry.isPlaying ? 'playing' : 'notplaying';
            });
        }
    };

    Ladder.prototype.deletePair = function (index) {
        var answer = confirm("Are you sure you want to delete this pair?");
        if (answer) {
            var api = new API();
            api.removePair(this.ladder[index].id, this.refreshLadder);
            hideModal(index);
        }
    };

    Ladder.prototype.setTime = function (index) {
        var api = new API();
        if(document.getElementById("checkbox8pm" + index).checked) {
            api.setTime("08:00 pm", this.ladder[index].id);
        } else {
            api.setTime("09:30 pm", this.ladder[index].id);
        }
    };

    Ladder.prototype.addPair = function (event) {
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

    Ladder.prototype.updatePair = function (index, event) {
        var api = new API();

        var pair = this.ladder[index];
        var pairId = pair.id;
        var newPosition = pair.newPosition;

        api.updatePairPosition(pairId, newPosition, this.refreshLadder);
        hideModal(index);
    };

    Ladder.prototype.updateLadder = function (ladderData) {
        this.ladder = ladderData;
        var ladderPages = [];
        if (ladderData) {
            var numPages = Math.floor(ladderData.length / NUM_ENTRIES_PER_PAGE) + 1;
            for (var i = 0; i < numPages; i++) {
                ladderPages[i] = [];
            }
            for (i = 0; i < ladderData.length; i++) {
                var pageIndex = Math.floor((ladderData[i].position - 1) / NUM_ENTRIES_PER_PAGE);
                ladderPages[pageIndex].push(ladderData[i]);
            }
        }
        this.ladderPages = ladderPages;
        this.currentPage = ladderPages[0];
    };

    Ladder.prototype.refreshLadder = function () {
        var api = new API();
        api.getLadder(function (ladderData) {
            this.updateLadder(ladderData);
            this.refreshMode();
        }.bind(this));
    };

    Ladder.prototype.refreshMode = function () {
        if (this.mode === 'edit') {
            this.ladder.forEach(function (entry) {
                entry.playingStatus = 'edit';
            });
        }
        else {
            this.ladder.forEach(function (entry) {
                entry.playingStatus = entry.isPlaying ? 'playing' : 'notplaying';
            });
        }
    };
    
    return Ladder;
})();