/* $Id$ */

final class DoneVoidOverridden extends Throwable {
    public Throwable fillInStackTrace() {
        return null;
    }
}