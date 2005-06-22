import ibis.colobus.*;

class PropertyTest {
    public static void main(String[] args) {
        Colobus colobus = Colobus.getColobus(PropertyTest.class.getName());

        colobus.fireEvent("test");

        long handle = colobus.fireStartEvent("start");
        colobus.fireStopEvent(handle, "stop");
    }
}
