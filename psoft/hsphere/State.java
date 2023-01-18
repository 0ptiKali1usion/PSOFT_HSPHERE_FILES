package psoft.hsphere;

import javax.servlet.http.HttpServletRequest;

/* loaded from: hsphere.zip:psoft/hsphere/State.class */
class State {
    User user;
    Account account;
    HttpServletRequest request;

    public State(User user, Account account, HttpServletRequest request) {
        this.user = user;
        this.account = account;
        this.request = request;
    }
}
