package thl.sentinel;

// The callback interface
public interface MyCallback {
    /*Show network configuration on UI when the network is available.*/
    void CBshowNetworkConfigurationOnUi();
    /*Start beacon server connection after the network is available.*/
    void CBstartBeaconServerConnection();
    /*Start to analysis scanned data when the network is available.*/
    void CBstartAnalysisReceiverData();
    /*Stop to analysis scanned data.*/
    void CBstopAnalysisAndUploadReceiverData();
    /*Recheck USB status.*/
    void CBrecheckUsbStatus();
    /*Show USB state on UI*/
    void CBshowUsbStatusOnUi();
    /*Show Server status on UI.*/
    void CBshowLocatorServerStatusOnUi(String response, boolean status);
    void CBshowBeaconServerStatusOnUi(String response, boolean status);
    /**/
    void CBsetBeaconCommand(String command);
    /*Update the result of beacon connection.*/
    void CBupdateAlarmCmdResultAndReConnectToServer(String mac, int result);
    /*Update the result of beacon bright.*/
    void CBupdateCmdResultAndReConnectToServer(String id, int result, String seq);
    /*Finish all actions.*/
    void CBsafeClose();
}
