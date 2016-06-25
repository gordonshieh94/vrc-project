/**
 * Created by David on 2016-06-15.
 */

var EDIT_BUTTON_HTML = '<a v-on:click="modalOpen($index)" class="edit-button btn btn-info btn-fab btn-fab-mini"><i class="material-icons md-light">create</i></a>';

Vue.component('matches', {
    template: '#matches-template',
    props: {
        active: "active",
        isActive: "isActive",
        matchlist: "matchlist",
        show: {
            type: Boolean,
            required: true,
            twoWay: true
        }
    },
    methods: {
        modalActiveContent: function(i) {
            return this.active === i;
        },
        setModalClose: function() {
            this.show = false;
            this.active = false;

        }
    }
});

var Matches = (function() {
    function Matches(matchData) {
        var matchHolderLeft = [];
        var matchHolderMid = [];
        var matchHolderRight = [];
        for (var match in matchData){
            var thisMatch = matchData[match];
            var matchColumn = thisMatch.matchNum % 3;
            switch(matchColumn){
                case 0:
                    matchHolderRight.push(thisMatch);
                    break;
                case 1:
                    matchHolderLeft.push(thisMatch);
                    break;
                case 2:
                    matchHolderMid.push(thisMatch);
            }

            var editButton = Vue.extend({
                 props: ['column','index'],
                 template: EDIT_BUTTON_HTML,
                 methods: {
                     modalOpen: function() {
                        if (this.column === "left") {
                            var modal = $("#matches-left,#modal" + this.index)[1];
                            modal.modal("show");
                        }
                        else if (column === "mid") {

                        }
                        else {

                        }
                        this.$parent.modalOpen(this.index);
                    }
                 }
             });
             Vue.component('edit-button', editButton);

             var blankButton = Vue.extend({
                template: "<a></a>"
            });
        }

        this.component = new Vue({
            el: '#matches',
            data: {
                active: 0,
                showModal: false,
                matchesLeft: matchHolderLeft,
                matchesMid: matchHolderMid,
                matchesRight: matchHolderRight,
                mode: 'read'
            },
            methods: {
                modalOpen: function(i) {
                    this.showModal = true;
                    this.active = i;
                    return this.active;
                }
//                 modalOpen2: function(i, name) {
//                    this.showModal = true;
//                    this.$refs[name].show = true;
//                    return this.active = i;
//                }
            },
            components: {
                edit: editButton,
                read: blankButton
            }
        })
    };

    Matches.prototype.changeMode = function() {
        if (this.mode === 'read') {
            this.mode = 'edit';
        }
        else {
            this.mode = 'read';
        }
    };

    return Matches;
})();