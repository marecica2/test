package payments;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import models.Event;

import org.apache.commons.collections.map.HashedMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import play.Logger;
import utils.DateTimeUtils;
import utils.StringUtils;

public class Paypal
{
    private String paypalEndpoint = "https://api-3t.sandbox.paypal.com/nvp";
    private String paymentUrl = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=";
    private String user = "marecica2-facilitator_api1.gmail.com";
    private String pwd = "1393232894";
    private String signature = "Ahi6toXd.Z09uBAi9TXUZIR3VEUjAI810NXwhh8lXf.N.aUDqMUKfbIe";

    private final String setExpressCheckout = "SetExpressCheckout";
    private final String getExpressCheckoutDetails = "GetExpressCheckoutDetails";
    private final String doExpressCheckout = "DoExpressCheckoutPayment";

    private final String version = "93";

    private BigDecimal paymentAmount = new BigDecimal("10");
    private String paymentCurrency = "USD";
    private String returnUrl;
    private String cancelUrl;
    private String providerPaypalAccount;
    private String providerPaypalAccountMicropayment;
    private String percentage;

    public Paypal(Event e, String returnUrl, String cancelUrl, String providerPaypalAccount, String providerPaypalAccountMicropayment, String user, String pwd, String signature,
            String endpoint, String paymentUrl, String percentage)
    {
        this.paymentAmount = e.price;
        this.paymentCurrency = e.currency;
        this.providerPaypalAccount = providerPaypalAccount;
        this.providerPaypalAccountMicropayment = providerPaypalAccountMicropayment;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;

        this.user = user;
        this.pwd = pwd;
        this.signature = signature;
        this.paypalEndpoint = endpoint;
        this.paymentUrl = paymentUrl;
        this.percentage = percentage;
    }

    public Paypal(Event e)
    {
        this.paymentAmount = e.price;
        this.paymentCurrency = e.currency;
    }

    public String getReturnUrl()
    {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl)
    {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl()
    {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl)
    {
        this.cancelUrl = cancelUrl;
    }

    public String getPaymentUrl(String token)
    {
        return paymentUrl + token;
    }

    public DoExpressCheckoutResponse doExpressCheckoutDual(String token, String payerId, Event event) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + URLEncoder.encode(doExpressCheckout, "UTF-8"));
        sb.append("&VERSION=" + URLEncoder.encode(version, "UTF-8"));
        sb.append("&TOKEN=" + token);
        sb.append("&PAYERID=" + payerId);

        DoExpressCheckoutResponse response = new DoExpressCheckoutResponse();
        processDetails(event, sb, response);

        //locale
        sb.append("&LC=" + "US");
        //locale
        sb.append("&LOCALECODE=" + "US");
        //hide shipping address
        sb.append("&NOSHIPPING=" + "1");
        // post all parameters back
        sb.append("&RM=" + 2);

        String resp = executeRequest(sb);
        System.err.println("-------------------");
        System.err.println("Paypal express checkout response for event " + event.uuid);
        System.err.println(URLDecoder.decode(resp.replaceAll("&", "\n"), "UTF-8"));
        System.err.println("-------------------");
        response.parseHttpResponse(URLDecoder.decode(resp, "UTF-8"));
        return response;
    }

    public AccessToken setExpressCheckoutDual(Event event) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + URLEncoder.encode(setExpressCheckout, "UTF-8"));
        sb.append("&VERSION=" + URLEncoder.encode(version, "UTF-8"));
        sb.append("&CANCELURL=" + URLEncoder.encode(cancelUrl, "UTF-8"));
        sb.append("&RETURNURL=" + URLEncoder.encode(returnUrl, "UTF-8"));
        sb.append("&RETURN=" + returnUrl);

        processDetails(event, sb, null);

        //locale
        sb.append("&LC=" + "US");
        //locale
        sb.append("&LOCALECODE=" + "US");
        //hide shipping address
        sb.append("&NOSHIPPING=" + "1");
        // post all parameters back
        sb.append("&RM=" + 2);

        String resp = executeRequest(sb);
        AccessToken token = new AccessToken();
        token.parseToken(resp);
        return token;
    }

    public String getExpressCheckoutDetail(String token) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + URLEncoder.encode(getExpressCheckoutDetails, "UTF-8"));
        sb.append("&VERSION=" + URLEncoder.encode(version, "UTF-8"));
        sb.append("&TOKEN=" + token);
        String resp = executeRequest(sb);
        return resp;
    }

    private void processDetails(Event event, StringBuilder sb, DoExpressCheckoutResponse response) throws UnsupportedEncodingException
    {
        final BigDecimal price = paymentAmount;
        final BigDecimal fee = price.multiply(new BigDecimal(percentage)).round(new MathContext(2));

        final BigDecimal providerPrice = price.subtract(fee);
        String paypalAccount = providerPaypalAccount;
        if (providerPrice.compareTo(new BigDecimal("5")) < 0)
            paypalAccount = providerPaypalAccountMicropayment;

        if (response != null)
        {
            response.providerPrice = providerPrice;
            response.fee = fee;
            response.price = price;
            response.account = event.user.account.paypalAccount;
            response.providerAccount = paypalAccount;
        }

        // provider payment
        sb.append("&PAYMENTREQUEST_0_PAYMENTACTION=" + URLEncoder.encode("Order", "UTF-8"));
        sb.append("&PAYMENTREQUEST_0_DESC=" + URLEncoder.encode(StringUtils.getStringNotNullMaxLen(event.listing.title, 100), "UTF-8"));

        //TODO important dont forget to switch it back
        //sb.append("&PAYMENTREQUEST_0_SELLERPAYPALACCOUNTID=" + event.account.paypalAccount);
        sb.append("&PAYMENTREQUEST_0_SELLERPAYPALACCOUNTID=" + "marek.balla@hotovo.org");
        sb.append("&PAYMENTREQUEST_0_CURRENCYCODE=" + URLEncoder.encode(paymentCurrency, "UTF-8"));
        sb.append("&PAYMENTREQUEST_0_AMT=" + URLEncoder.encode(providerPrice.toPlainString(), "UTF-8"));
        sb.append("&PAYMENTREQUEST_0_PAYMENTREQUESTID=" + URLEncoder.encode(event.id + "_provider", "UTF-8"));

        // our payment
        sb.append("&PAYMENTREQUEST_1_PAYMENTACTION=" + URLEncoder.encode("Order", "UTF-8"));
        sb.append("&PAYMENTREQUEST_1_DESC=" + URLEncoder.encode(StringUtils.getStringNotNullMaxLen(event.listing.title, 100), "UTF-8"));
        //sb.append("&PAYMENTREQUEST_1_SELLERPAYPALACCOUNTID=" + paypalAccount);
        sb.append("&PAYMENTREQUEST_1_SELLERPAYPALACCOUNTID=" + "marecica22@yahoo.com");
        sb.append("&PAYMENTREQUEST_1_CURRENCYCODE=" + URLEncoder.encode(paymentCurrency, "UTF-8"));
        sb.append("&PAYMENTREQUEST_1_AMT=" + URLEncoder.encode(fee.toPlainString(), "UTF-8"));
        sb.append("&PAYMENTREQUEST_1_PAYMENTREQUESTID=" + URLEncoder.encode(event.id + "_our", "UTF-8"));

        // item details
        sb.append("&L_PAYMENTREQUEST_0_NAME0=" + StringUtils.getStringNotNullMaxLen(event.listing.title, 100));
        //sb.append("&L_PAYMENTREQUEST_0_DESC0=" + StringUtils.getStringNotNullMaxLen(event.description, 100));
        sb.append("&L_PAYMENTREQUEST_0_AMT0=" + URLEncoder.encode(providerPrice.toPlainString(), "UTF-8"));

        sb.append("&L_PAYMENTREQUEST_1_NAME0=" + "Servise provider fee");
        sb.append("&L_PAYMENTREQUEST_1_AMT0=" + URLEncoder.encode(fee.toPlainString(), "UTF-8"));
    }

    private String executeRequest(StringBuilder sb) throws UnsupportedEncodingException, IOException, ClientProtocolException
    {
        HttpEntity entity = new StringEntity(sb.toString(), "UTF-8");
        HttpPost post = new HttpPost(paypalEndpoint);
        post.setEntity(entity);
        post.setHeader(new BasicHeader("Content-type", "application/x-www-form-urlencoded"));
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(post);
        String resp = EntityUtils.toString(response.getEntity());
        Logger.error("========= ");
        Logger.error("Paypal response " + resp);
        return resp;
    }

    public class DoExpressCheckoutResponse
    {
        public BigDecimal fee;
        public boolean success = false;
        public String errorMessage = "";
        public String transactionIdProvider = "";
        public String transactionIdOur = "";
        public BigDecimal price = null;
        public BigDecimal providerPrice = null;
        public String providerAccount;
        public String account;

        public void parseHttpResponse(String resp) throws Exception
        {
            String[] pairs = resp.split("&");
            Map<String, String> responseMap = new HashedMap();
            for (int i = 0; i < pairs.length; i++)
            {
                String[] pair = pairs[i].split("=");
                responseMap.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
            }

            if ("Success".equals(responseMap.get("ACK")))
                success = true;
            else
            {
                success = false;
                errorMessage = "STATUS: " +
                        responseMap.get("ACK") + " Message: " +
                        StringUtils.getStringNotNull(responseMap.get("PAYMENTINFO_1_SHORTMESSAGE")) + " " +
                        StringUtils.getStringNotNull(responseMap.get("PAYMENTINFO_1_LONGMESSAGE")) + " " +
                        StringUtils.getStringNotNull(responseMap.get("PAYMENTINFO_2_SHORTMESSAGE")) + " " +
                        StringUtils.getStringNotNull(responseMap.get("PAYMENTINFO_2_LONGMESSAGE")) + " " +
                        StringUtils.getStringNotNull(responseMap.get("L_SHORTMESSAGE0")) + " " +
                        StringUtils.getStringNotNull(responseMap.get("L_LONGMESSAGE0")) + " " +
                        "";
                errorMessage = URLDecoder.decode(errorMessage, "UTF-8");
            }

            if (responseMap.containsKey("PAYMENTINFO_0_TRANSACTIONID"))
                transactionIdProvider = responseMap.get("PAYMENTINFO_0_TRANSACTIONID");

            if (responseMap.containsKey("PAYMENTINFO_1_TRANSACTIONID"))
                transactionIdOur = responseMap.get("PAYMENTINFO_1_TRANSACTIONID");
        }

        @Override
        public String toString()
        {
            return "DoExpressCheckoutResponse [success=" + success + ", errorMessage=" + errorMessage + ", transactionIdProvider=" + transactionIdProvider + ", transactionIdOur="
                    + transactionIdOur + "]";
        }

    }

    public class AccessToken
    {
        private String token = "";
        private Date validity = null;

        public String getToken()
        {
            return token;
        }

        public void setToken(String token)
        {
            try
            {
                this.token = URLDecoder.decode(token, "UTF-8");
            } catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        }

        public Date getValidity()
        {
            return validity;
        }

        public void setValidity(String validity)
        {
            DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_PAYPAL);
            try
            {
                validity = URLDecoder.decode(validity, "UTF-8");
            } catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            Date d = dt.fromString(validity);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.add(Calendar.HOUR, 2);
            this.validity = c.getTime();
        }

        @Override
        public String toString()
        {
            return "AccessToken [token=" + token + ", validity=" + validity + "]";
        }

        public void parseToken(String resp)
        {
            String[] pairs = resp.split("&");
            for (int i = 0; i < pairs.length; i++)
            {
                String[] pair = pairs[i].split("=");
                String key = pair[0];
                String value = pair[1];

                if (key.equals("TOKEN"))
                    this.setToken(value);

                if (key.equals("TIMESTAMP"))
                    this.setValidity(value);
            }
        }

    }

    //    public String doExpressCheckout(String token, String payerId, Event event) throws Exception
    //    {
    //        StringBuilder sb = new StringBuilder();
    //        sb.append("USER=" + user);
    //        sb.append("&PWD=" + pwd);
    //        sb.append("&SIGNATURE=" + signature);
    //        sb.append("&METHOD=" + URLEncoder.encode(doExpressCheckout, "UTF-8"));
    //        sb.append("&VERSION=" + URLEncoder.encode(version, "UTF-8"));
    //        sb.append("&TOKEN=" + token);
    //        sb.append("&PAYERID=" + payerId);
    //        sb.append("&PAYMENTREQUEST_0_PAYMENTACTION=" + URLEncoder.encode(paymentAction, "UTF-8"));
    //        sb.append("&PAYMENTREQUEST_0_AMT=" + URLEncoder.encode(paymentAmount, "UTF-8"));
    //        sb.append("&PAYMENTREQUEST_0_CURRENCYCODE=" + URLEncoder.encode(paymentCurrency, "UTF-8"));
    //
    //        String resp = executeRequest(sb);
    //        return resp;
    //    }

    //    public AccessToken getAccessToken(Event event) throws Exception
    //    {
    //        StringBuilder sb = new StringBuilder();
    //        sb.append("USER=" + user);
    //        sb.append("&PWD=" + pwd);
    //        sb.append("&SIGNATURE=" + signature);
    //        sb.append("&METHOD=" + URLEncoder.encode(setExpressCheckout, "UTF-8"));
    //        sb.append("&VERSION=" + URLEncoder.encode(version, "UTF-8"));
    //
    //        // item
    //        sb.append("&L_PAYMENTREQUEST_0_NAME0=" + event.title);
    //        sb.append("&L_PAYMENTREQUEST_0_NUMBER0=" + event.id);
    //        sb.append("&L_PAYMENTREQUEST_0_DESC0=" + event.description);
    //        sb.append("&L_PAYMENTREQUEST_0_AMT0=" + URLEncoder.encode(paymentAmount, "UTF-8"));
    //        sb.append("&L_PAYMENTREQUEST_0_QTY0=" + "1");
    //
    //        // total
    //        sb.append("&PAYMENTREQUEST_0_PAYMENTACTION=" + URLEncoder.encode(paymentAction, "UTF-8"));
    //        sb.append("&PAYMENTREQUEST_0_CURRENCYCODE=" + URLEncoder.encode(paymentCurrency, "UTF-8"));
    //        sb.append("&PAYMENTREQUEST_0_AMT=" + URLEncoder.encode(paymentAmount, "UTF-8"));
    //
    //        sb.append("&CANCELURL=" + URLEncoder.encode(cancelUrl, "UTF-8"));
    //        sb.append("&RETURNURL=" + URLEncoder.encode(returnUrl, "UTF-8"));
    //        sb.append("&RETURN=" + returnUrl);
    //
    //        //
    //        // extra parameters
    //        //
    //
    //        //locale
    //        sb.append("&LC=" + "US");
    //        //locale
    //        sb.append("&LOCALECODE=" + "US");
    //        //hide shipping address
    //        sb.append("&NOSHIPPING=" + "1");
    //        // post all parameters back
    //        sb.append("&RM=" + 2);
    //
    //        String resp = executeRequest(sb);
    //        AccessToken token = new AccessToken();
    //        token.parseToken(resp);
    //        return token;
    //    }
}
