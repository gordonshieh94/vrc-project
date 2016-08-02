(function () {
    "use strict";

    $.material.options.validate = false;
    $.material.init();

    var PASS_RECOVERY_FORM_ID = "#recoveryForm";

    // Override the submit button redirect
    $(PASS_RECOVERY_FORM_ID).submit(function () {
        return false;
    });

    var onEmailSubmit = function() {
        recoveryForm.emailRetrieved = true;
        $("#submitEmailButton").prop("disabled", true);
        this.spinnerVisibilityTop = true;
        var api = new API();
        api.getUserSecurityQuestion(this.email, onGotQuestion, onGotQuestionError.bind(this));
    };

    var onGotQuestion = function (securityQuestion){
        console.log("Hello!");
        this.spinnerVisibilityTop = false;
        recoveryForm.securityQuestion = securityQuestion;
    };

    var onGotQuestionError = function (response) {
        console.log("Hello!2");
        this.spinnerVisibilityTop = false;
        if (response.status == 401) {
            this.invalidCredentials = true;
            $("#inputEmail").parent().addClass("has-error");
            $("#inputEmail").focus();
        }
    };

    var onAnswerSubmit = function() {
        this.questionAnswered = true;
        this.spinnerVisibilityMid = true;
        var api = new API();
        api.answerSecurityQuestion(this.email, this.securityAnswer, onAnsweredQuestion, onAnsweredQuestionError.bind(this));
    };

    var onAnsweredQuestion = function (voucherCode) {
        this.spinnerVisibilityMid = false;
        recoveryForm.questionAnswered = true;
        this.voucherCode = voucherCode;
    };

     var onAnsweredQuestionError = function (response) {
         this.spinnerVisibilityMid = false;
         if (response.status == 401) {
             this.invalidCredentials = true;
             $("#inputSecurityAnswer").parent().addClass("has-error");
             $("#inputSecurityAnswer").focus();
         }
     };

     var onPasswordSubmit = function () {
        if (this.password === this.passwordConfirm){
            this.spinnerVisibilityBottom = true;
            var api = new API();
            api.changePassword(this.email, this.voucherCode, this.password, onChangedPassword, onChangedPasswordError.bind(this));
        } else {
            $("#submitPasswordButton").prop("disabled", true);
            alert("Passwords must match.");
        }
     };

     var onChangedPassword = function () {
         this.spinnerVisibilityBottom = false;
         alert("Password successfully changed! Please log in.");
     };

     var onChangedPasswordError = function (response) {
         this.spinnerVisibilityBottom = false;
         if (response.status == 401) {
             this.invalidCredentials = true;
             $("#inputPassword").parent().addClass("has-error");
             $("#inputPassword").focus();
         }
     };

    Vue.validator('email', function (val) {
        return /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/.test(val);
    });

    Vue.validator('passwordConfirm', function (val) {
        if (recoveryForm){
            return (recoveryForm.password === val);
        } else {
            return false;
        }
    });

    Vue.validator('minLength', function (val) {
        return /^.{6,}$/.test(val);
    });

    var onValid = function() {
        console.log(this.$emailValidator.touched);
        console.log(this.$securityValidator.touched);
        console.log(this.$passwordValidator.touched);
        if (this.$emailValidator.touched && !recoveryForm.emailRetrieved && !recoveryForm.questionAnswered) {
            $("#submitEmailButton").prop("disabled", false);
        } else if (this.$passwordValidator.touched && recoveryForm.emailRetrieved && !recoveryForm.questionAnswered) {
            console.log("sec valid");
            $("#submitAnswerButton").prop("disabled", false);
        } else if (this.$passwordValidator.touched && recoveryForm.emailRetrieved && recoveryForm.questionAnswered) {
            $("#submitPasswordButton").prop("disabled", false);
        }
    };

    var onInvalid = function() {
        if (!recoveryForm.emailRetrieved) {
            $("#submitEmailButton").prop("disabled", true);
        } else {
            $("#submitAnswerButton").prop("disabled", true);
        }
    };

    var recoveryForm = new Vue({
        el: PASS_RECOVERY_FORM_ID,
        components: {
            'ClipLoader': VueSpinner.ClipLoader
        },
        data: {
            email: "",
            password: "",
            passwordConfirm: "",
            securityQuestion: "",
            securityAnswer: "",
            voucherCode: "",
            emailRetrieved: false,
            questionAnswered: false,
            color: '#03a9f4',
            spinnerVisibilityTop: false,
            spinnerVisibilityMid: false,
            spinnerVisibilityBottom: false
        },
        methods: {
            onAnswerSubmit: onAnswerSubmit,
            onEmailSubmit: onEmailSubmit,
            onValid: onValid,
            onInvalid: onInvalid,
            onPasswordSubmit: onPasswordSubmit,
            openModal: function () {
                console.log("Hey!");
                $("#forgPassModal").modal("show");
            }
        }
    });


})();
