package psoft.hsphere.PGPSecurity;

import psoft.hsphere.Session;
import psoft.hsphere.resource.p004tt.Ticket;

/* loaded from: hsphere.zip:psoft/hsphere/PGPSecurity/SignificationFilter.class */
public class SignificationFilter {
    String stringUnderFiltration;

    /* loaded from: hsphere.zip:psoft/hsphere/PGPSecurity/SignificationFilter$MessageStorage.class */
    public class MessageStorage {
        private String message;
        private String start;
        private String end;

        /* loaded from: hsphere.zip:psoft/hsphere/PGPSecurity/SignificationFilter$MessageStorage$ItemStructure.class */
        public class ItemStructure {
            public String type;
            public int position;

            public ItemStructure(String type, int position) {
                MessageStorage.this = r4;
                this.type = type;
                this.position = position;
            }
        }

        public MessageStorage(String s, String start, String end) {
            SignificationFilter.this = r4;
            this.message = s;
            this.start = start;
            this.end = end;
        }

        public String getBlock() {
            int startIndex = 0;
            int currentIndex = 0;
            int depth = 0;
            boolean theFirst = true;
            while (getNext(currentIndex) != null) {
                ItemStructure is = getNext(currentIndex);
                if (is.type.equals(this.start)) {
                    if (theFirst) {
                        theFirst = false;
                        startIndex = is.position;
                    }
                    currentIndex = is.position + 1;
                    depth++;
                }
                if (is.type.equals(this.end)) {
                    if (theFirst) {
                        return null;
                    }
                    depth--;
                    if (depth == 0) {
                        int endIndex = is.position + this.end.length();
                        String block = new String(this.message.substring(startIndex, endIndex));
                        return block;
                    }
                    currentIndex = is.position + 1;
                }
            }
            return null;
        }

        private ItemStructure getNext(int fromIndex) {
            int t_start = this.message.indexOf(this.start, fromIndex);
            int t_end = this.message.indexOf(this.end, fromIndex);
            if (t_start == -1 && t_end == -1) {
                return null;
            }
            if (t_start == -1) {
                return new ItemStructure(this.end, t_end);
            }
            if (t_end == -1) {
                return new ItemStructure(this.start, t_start);
            }
            if (Math.min(t_start, t_end) == t_end) {
                return new ItemStructure(this.end, t_end);
            }
            return new ItemStructure(this.start, t_start);
        }

        public void replace(String part, String onto) {
            int enterStartIndex = this.message.indexOf(part);
            if (enterStartIndex == -1) {
                return;
            }
            int enterEndIndex = enterStartIndex + part.length();
            String resultString = this.message.substring(0, enterStartIndex) + onto + this.message.substring(enterEndIndex);
            this.message = resultString;
        }

        public String getData() {
            return this.message;
        }
    }

    private SignificationFilter(String s) {
        this.stringUnderFiltration = null;
        this.stringUnderFiltration = s;
    }

    private String getFiltered() throws Exception {
        MessageStorage storage = new MessageStorage(this.stringUnderFiltration, Ticket.PGP_SIGNIFICATION_BEGIN, Ticket.PGP_SIGNIFICATION_END);
        while (true) {
            String block = storage.getBlock();
            if (block != null) {
                String unsigned = C0007PGPSecurity.unsignMessage(block);
                storage.replace(block, unsigned);
            } else {
                return storage.getData();
            }
        }
    }

    public static String getFiltered(String s) throws Exception {
        if (s == null || s.equals("")) {
            return s;
        }
        try {
            String result = new String(new SignificationFilter(s).getFiltered());
            if (result.equals("")) {
                result = new String("The message has no content.");
            }
            return result;
        } catch (ClassCastException e) {
            Session.getLog().error("Message contains PGP signed message with incorrect format");
            return s;
        } catch (Exception e2) {
            Session.getLog().error("Cannot filter message");
            return s;
        }
    }
}
