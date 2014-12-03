<%@ page import="java.io.File,
                 java.util.List,
                 org.jivesoftware.openfire.XMPPServer,
                 org.jivesoftware.util.*,
                 com.wecapslabs.openfire.plugin.apns.ApnsPlugin"
    errorPage="error.jsp"
%>

<%  // Get parameters
    boolean save = request.getParameter("save") != null;

    ApnsPlugin plugin = (ApnsPlugin) XMPPServer.getInstance().getPluginManager().getPlugin("openfire-apns");

    if (save) {
        plugin.setCertificatePath(ParamUtils.getParameter(request, "certificatePath"));
        plugin.setPassword(ParamUtils.getParameter(request, "password"));
        plugin.setBadge(ParamUtils.getParameter(request, "badge"));
        plugin.setSound(ParamUtils.getParameter(request, "sound"));
        plugin.setProduction(ParamUtils.getParameter(request, "production"));
        plugin.setSendMessageBody(ParamUtils.getParameter(request, "sendMessageBody"));
        plugin.setMessageBodyPlaceholder(ParamUtils.getParameter(request, "messageBodyPlaceholder"));
    }

    String certificatePath = plugin.getCertificatePath();
    String password = plugin.getPassword();
    String sound = plugin.getSound();
    int badge = plugin.getBadge();
    boolean production = plugin.getProduction();
    boolean sendMessageBody = plugin.getSendMessageBody();
    String messageBodyPlaceholder = plugin.getMessageBodyPlaceholder();
%>

<html>
    <head>
        <title>APNS Settings Properties</title>
        <meta name="pageID" content="apns-settings"/>
    </head>
    <body>

<form action="apns.jsp?save" method="post">
<div class="jive-contentBoxHeader">APNS settings</div>
<div class="jive-contentBox">
    <label for="file">p12 Certificate path on server:</label>
    <input type="text" name="certificatePath" value="<%= certificatePath %>" size="50"/>
    <br>

    <label for="password">Certificate password:</label>
    <input type="password" name="password" value="<%= password %>" />
    <br>

    <label for="badge">Payload badge</label>
    <input type="text" name="badge" value="<%= badge %>" />
    <br>

    <label for="sound">Payload sound</label>
    <input type="text" name="sound" value="<%= sound %>" />
    <br>

    <input type="radio" name="production" value="false" <%= production ? "" : "checked" %>>Sandbox
    <input type="radio" name="production" value="true" <%= production ? "checked" : "" %>>Production
    <br>

    <label for="sendMessageBody">Send message text with notification:</label>
    <input type="radio" name="sendMessageBody" value="true" <%= sendMessageBody ? "checked" : "" %>>Yes
    <input type="radio" name="sendMessageBody" value="false" <%= sendMessageBody ? "" : "checked" %>>No
    <br>

    <label for="messageBodyPlaceholder">Message text placeholder:</label>
    <input type="text" name="messageBodyPlaceholder" value="<%= messageBodyPlaceholder %>" />
    <br>

    <input type="submit" value="Save">
</div>
</form>

</body>
</html>
