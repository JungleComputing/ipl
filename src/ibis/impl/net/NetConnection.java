package ibis.ipl.impl.net;

public final class NetConnection {
        private NetPort                  port           = null;
        private Integer                  num            = null;
        private NetSendPortIdentifier    sendId         = null;
        private NetReceivePortIdentifier receiveId      = null;
        private NetServiceLink           serviceLink    = null;

        public NetConnection(NetPort                  port       ,
                             Integer                  num        ,
                             NetSendPortIdentifier    sendId     ,
                             NetReceivePortIdentifier receiveId  ,
                             NetServiceLink           serviceLink) {
                this.port        = port       ;
                this.num         = num        ;
                this.sendId      = sendId     ;
                this.receiveId   = receiveId  ;
                this.serviceLink = serviceLink;
        }

        public NetConnection(NetConnection            model,
                             Integer                  newnum) {
                this(model.port, newnum, model.sendId, model.receiveId, model.serviceLink);
        }

        public synchronized NetPort                  getPort       () {
                return port;
        }
        
        public synchronized Integer                  getNum        () {
                return num;
        }
        
        public synchronized NetSendPortIdentifier    getSendId     () {
                return sendId;
        }
        
        public synchronized NetReceivePortIdentifier getReceiveId  () {
                return receiveId;
        }
        
        public synchronized NetServiceLink           getServiceLink() {
                return serviceLink;
        }

        public synchronized void close()  throws NetIbisException {
                if (serviceLink != null) {
                        serviceLink.close();
                }
                
                port        = null;
                num         = null;
                sendId      = null;
                receiveId   = null;
                serviceLink = null;
        }
}
