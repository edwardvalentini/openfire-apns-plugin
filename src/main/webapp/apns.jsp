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
    }

    String certificatePath = plugin.getCertificatePath();
    String password = plugin.getPassword();
    String badge = Integer.toString(plugin.getBadge());
    String sound = plugin.getSound();
    String production = plugin.getProduction() ? "true" : "false";
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

    <input type="radio" name="production" value="false" <%= production.equals("true") ? "" : "checked" %>>Sandbox
    <input type="radio" name="production" value="true" <%= production.equals("true") ? "checked" : "" %>>Production
    <br>

    <input type="submit" value="Save">
</div>
</form>

</body>
</html>
