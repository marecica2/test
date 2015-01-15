package payments;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import models.AccountPlan;
import models.Event;
import models.User;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import play.Logger;
import play.i18n.Messages;
import utils.DateTimeUtils;
import utils.StringUtils;
import utils.UriUtils;
import controllers.BaseController;

public class Paypal
{
    private String paypalEndpoint = "https://api-3t.sandbox.paypal.com/nvp";
    private String paymentUrl = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=";
    private String user = "marecica2-facilitator_api1.gmail.com";
    private String pwd = "1393232894";
    private String signature = "Ahi6toXd.Z09uBAi9TXUZIR3VEUjAI810NXwhh8lXf.N.aUDqMUKfbIe";

    private final String setExpressCheckout = "SetExpressCheckout";
    private final String doExpressCheckout = "DoExpressCheckoutPayment";
    private final String version = "93";

    private BigDecimal paymentAmount = new BigDecimal("0");
    private String paymentCurrency = "USD";
    private String returnUrl;
    private String cancelUrl;
    private String providerPaypalAccount;
    private String providerPaypalAccountMicropayment;
    private String percentage;

    public Paypal(Event e, String returnUrl, String cancelUrl, String providerPaypalAccount, String providerPaypalAccountMicropayment, String user, String pwd, String signature,
            String endpoint, String paymentUrl, String percentage)
    {
        if (e != null)
        {
            this.paymentAmount = e.getTotalPrice();
            this.paymentCurrency = e.currency;
        }
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

    public Paypal(String returnUrl, String cancelUrl, String user, String password, String signature, BigDecimal price, String currency)
    {
        this.paymentAmount = price;
        this.paymentCurrency = currency;
        this.user = user;
        this.signature = signature;
        this.pwd = password;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
    }

    public Paypal(Event e)
    {
        this.paymentAmount = e.getTotalPrice();
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

    public Map<String, String> doAdaptiveCheckoutRefund(String payKey) throws Exception
    {
        List<Header> headers = adaptiveHeaders();
        StringBuilder sb = new StringBuilder();
        sb.append("payKey=" + UriUtils.urlEncode(payKey));
        sb.append("&requestEnvelope.errorLanguage=" + UriUtils.urlEncode("en_US"));
        String resp = adaptiveExecuteRequest(headers, sb, BaseController.getProperty(BaseController.CONFIG_PAYPAL_ADAPTIVE_REFUND_URL));

        Logger.info("");
        Logger.info("-------------- doAdaptiveCheckoutRefund response -----------------");
        Logger.info(resp);
        return new PaypalResponseParser().parse(resp);
    }

    public Map<String, String> setAdaptiveCheckoutOptions(Event event, Boolean dual, String payKey) throws Exception
    {
        List<Header> headers = adaptiveHeaders();
        StringBuilder sb = new StringBuilder();
        sb.append("payKey=" + UriUtils.urlEncode(payKey));
        sb.append("&requestEnvelope.errorLanguage=" + UriUtils.urlEncode("en_US"));
        sb.append("&receiverOptions[0].receiver.email=" + UriUtils.urlEncode(event.user.account.paypalAccount));
        sb.append("&receiverOptions[0].description=" + UriUtils.urlEncode(StringUtils.getStringNotNullMaxLen(event.listing.title, 100)));

        if (dual)
        {
            sb.append("&receiverOptions[1].receiver.email=" + UriUtils.urlEncode(providerPaypalAccount));
            sb.append("&receiverOptions[1].description=" + UriUtils.urlEncode(StringUtils.getStringNotNullMaxLen(event.listing.title, 100)));
        }

        String resp = adaptiveExecuteRequest(headers, sb, BaseController.getProperty(BaseController.CONFIG_PAYPAL_ADAPTIVE_OPTIONS_URL));

        Logger.info("");
        Logger.info("-------------- setAdaptivePaymentOptions response -----------------");
        Logger.info(resp);
        return new PaypalResponseParser().parse(resp);
    }

    public Map<String, String> getAdaptiveCheckoutDetails(Event event, Boolean dual, String payKey) throws Exception
    {
        List<Header> headers = adaptiveHeaders();

        StringBuilder sb = new StringBuilder();
        sb.append("payKey=" + UriUtils.urlEncode(payKey));
        sb.append("&requestEnvelope.errorLanguage=" + UriUtils.urlEncode("en_US"));

        String resp = adaptiveExecuteRequest(headers, sb, BaseController.getProperty(BaseController.CONFIG_PAYPAL_ADAPTIVE_DETAILS_URL));

        Logger.info("");
        Logger.info("-------------- setAdaptivePaymentOptions response -----------------");
        Logger.info(resp);

        return new PaypalResponseParser().parse(resp);
    }

    public Map<String, String> setAdaptiveCheckout(Event event, Boolean dual) throws Exception
    {
        final BigDecimal price = paymentAmount;
        final BigDecimal fee = dual ? price.multiply(new BigDecimal(percentage)).round(new MathContext(2)) : new BigDecimal(0);
        final BigDecimal providerPrice = price.subtract(fee);
        String paypalAccount = providerPaypalAccount;
        if (providerPrice.compareTo(new BigDecimal("3")) < 0)
            paypalAccount = providerPaypalAccountMicropayment;

        List<Header> headers = adaptiveHeaders();

        StringBuilder sb = new StringBuilder();
        sb.append("actionType=" + UriUtils.urlEncode("PAY"));
        sb.append("&returnUrl=" + UriUtils.urlEncode(returnUrl));
        sb.append("&cancelUrl=" + UriUtils.urlEncode(cancelUrl));
        sb.append("&requestEnvelope.errorLanguage=" + UriUtils.urlEncode("en_US"));

        // provider payment
        sb.append("&currencyCode=" + UriUtils.urlEncode(paymentCurrency));
        sb.append("&receiverList.receiver(0).email=" + event.user.account.paypalAccount);
        sb.append("&receiverList.receiver(0).amount=" + UriUtils.urlEncode(providerPrice.toPlainString()));

        // our payment
        if (dual)
        {
            sb.append("&receiverList.receiver(1).email=" + paypalAccount);
            sb.append("&receiverList.receiver(1).amount=" + UriUtils.urlEncode(fee.toPlainString()));
        }

        String resp = adaptiveExecuteRequest(headers, sb, BaseController.getProperty(BaseController.CONFIG_PAYPAL_ADAPTIVE_ENDPOINT));

        Logger.info("");
        Logger.info("-------------- setAdaptiveCheckout response -----------------");
        Logger.info(resp);

        return new PaypalResponseParser().parse(resp);
    }

    public PaypalResponseParser doExpressCheckoutDual(String token, String payerId, Event event, Boolean dual) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + UriUtils.urlEncode(doExpressCheckout));
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&TOKEN=" + token);
        sb.append("&PAYERID=" + payerId);

        PaypalResponseParser response = new PaypalResponseParser();
        processDetails(event, sb, response, dual);

        //locale
        //sb.append("&LC=" + "US");
        //locale
        //sb.append("&LOCALECODE=" + "US");
        //hide shipping address
        //sb.append("&NOSHIPPING=" + "1");
        // post all parameters back
        sb.append("&RM=" + 2);

        String resp = executeRequest(sb);
        Logger.info("");
        Logger.info("----------- doExpressCheckoutDual response  -----------");
        Logger.info(UriUtils.urlDecode(resp.replaceAll("&", "\n")));

        response.parseHttpResponse(UriUtils.urlDecode(resp));
        return response;
    }

    public AccessToken setExpressCheckoutDual(Event event, Boolean dual) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + UriUtils.urlEncode(setExpressCheckout));
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&CANCELURL=" + UriUtils.urlEncode(cancelUrl));
        sb.append("&RETURNURL=" + UriUtils.urlEncode(returnUrl));
        sb.append("&RETURN=" + UriUtils.urlEncode(returnUrl));

        processDetails(event, sb, null, dual);

        //locale
        //sb.append("&LC=" + "US");
        //locale
        //sb.append("&LOCALECODE=" + "US");
        //hide shipping address
        //sb.append("&NOSHIPPING=" + "1");
        // post all parameters back
        sb.append("&RM=" + 2);

        String resp = executeRequest(sb);
        AccessToken token = new AccessToken();
        token.parseToken(resp);
        return token;
    }

    public Map<String, String> setTransactionStatus(String transactionId) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + UriUtils.urlEncode("ManagePendingTransactionStatus"));
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&TRANSACTIONID=" + UriUtils.urlEncode(transactionId));
        sb.append("&ACTION=" + UriUtils.urlEncode("Accept"));

        PaypalResponseParser parser = new PaypalResponseParser();
        String resp = executeRequest(sb);
        Logger.info("");
        Logger.info("----------- setTransactionStatus response  -----------");
        Logger.info(resp);

        Map<String, String> params = parser.parse(resp);
        return params;
    }

    public AccessToken setRecurring(User usr, AccountPlan currentPlan) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + UriUtils.urlEncode(setExpressCheckout));
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&L_BILLINGTYPE0=" + UriUtils.urlEncode("RecurringPayments"));
        sb.append("&L_BILLINGAGREEMENTDESCRIPTION0=" + UriUtils.urlEncode("Widgr - Subscription for plan: " + Messages.get(currentPlan.type)));
        sb.append("&PAYMENTREQUEST_0_AMT=" + UriUtils.urlEncode(currentPlan.price.toPlainString()));
        sb.append("&CANCELURL=" + UriUtils.urlEncode(cancelUrl));
        sb.append("&RETURNURL=" + UriUtils.urlEncode(returnUrl));
        sb.append("&NOSHIPPING=" + "1");

        String resp = executeRequest(sb);
        Logger.info("");
        Logger.info("----------- setRecurring response -----------");
        Logger.info(resp);
        AccessToken token = new AccessToken();
        token.parseToken(resp);
        return token;
    }

    public String doRecurring(User usr, String token, AccountPlan currentPlan) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&METHOD=" + UriUtils.urlEncode("CreateRecurringPaymentsProfile"));
        sb.append("&TOKEN=" + UriUtils.urlEncode(token));
        sb.append("&NOSHIPPING=" + "1");

        //expected format 2015-01-15T10:00:00Z
        SimpleDateFormat fDate = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DAY_OF_MONTH, 1);
        String date = fDate.format(c.getTime()) + "T00:00:00Z";

        sb.append("&PROFILESTARTDATE=" + UriUtils.urlEncode(date));
        sb.append("&BILLINGPERIOD=" + UriUtils.urlEncode("Month"));
        sb.append("&BILLINGFREQUENCY=" + UriUtils.urlEncode("1"));
        sb.append("&DESC=" + UriUtils.urlEncode("Widgr - Subscription for plan: " + Messages.get(currentPlan.type)));
        sb.append("&AMT=" + UriUtils.urlEncode(currentPlan.price.toPlainString()));
        sb.append("&CURRENCYCODE=" + UriUtils.urlEncode("USD"));
        sb.append("&COUNTRYCODE=" + UriUtils.urlEncode("US"));
        sb.append("&MAXFAILEDPAYMENTS=" + UriUtils.urlEncode("1"));
        sb.append("&L_PAYMENTREQUEST_0_ITEMCATEGORY0=" + UriUtils.urlEncode("Digital"));
        sb.append("&L_PAYMENTREQUEST_0_AMT0=" + UriUtils.urlEncode(currentPlan.price.toPlainString()));
        sb.append("&L_PAYMENTREQUEST_0_QTY0=" + UriUtils.urlEncode("1"));
        sb.append("&L_PAYMENTREQUEST_0_NAME0=" + UriUtils.urlEncode("Widgr - Subscription for plan: " + Messages.get(currentPlan.type)));

        PaypalResponseParser parser = new PaypalResponseParser();
        String resp = executeRequest(sb);
        Logger.info("");
        Logger.info("----------- executeRecurring response -----------");
        Logger.info(resp);

        Map<String, String> params = parser.parse(resp);
        String profileId = params.get("PROFILEID");
        if (profileId != null)
            return profileId;
        return null;
    }

    public Map<String, String> getRecurringPayments(String profile)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + UriUtils.urlEncode("GetRecurringPaymentsProfileDetails"));
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&PROFILEID=" + UriUtils.urlEncode(profile));

        PaypalResponseParser parser = new PaypalResponseParser();
        String resp = executeRequest(sb);
        Logger.info("");
        Logger.info("----------- getRecurringPayments response -----------");
        Logger.info(resp);

        Map<String, String> map = parser.parse(resp);
        return map;
    }

    public Map<String, String> cancelRecurringPayments(User usr, AccountPlan plan)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + UriUtils.urlEncode("ManageRecurringPaymentsProfileStatus"));
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&ACTION=" + UriUtils.urlEncode("Cancel"));
        sb.append("&PROFILEID=" + UriUtils.urlEncode(plan.profile));

        PaypalResponseParser parser = new PaypalResponseParser();
        String resp = executeRequest(sb);
        Logger.info("");
        Logger.info("----------- cancelRecurringPayments response -----------");
        Logger.info(resp);

        Map<String, String> map = parser.parse(resp);
        return map;
    }

    public Map<String, String> refund(String payerId, String transactionId) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER=" + user);
        sb.append("&PWD=" + pwd);
        sb.append("&SIGNATURE=" + signature);
        sb.append("&METHOD=" + UriUtils.urlEncode("RefundTransaction"));
        sb.append("&VERSION=" + UriUtils.urlEncode(version));
        sb.append("&PAYERID=" + UriUtils.urlEncode(payerId));
        sb.append("&TRANSACTIONID=" + UriUtils.urlEncode(transactionId));
        sb.append("&REFUNDTYPE=" + UriUtils.urlEncode("Full"));
        sb.append("&REFUNDSOURCE=" + UriUtils.urlEncode("instant"));

        PaypalResponseParser parser = new PaypalResponseParser();
        String resp = executeRequest(sb);
        Logger.info("");
        Logger.info("----------- refund response -----------");
        Logger.info(resp);

        Map<String, String> map = parser.parse(resp);
        return map;
    }

    private void processDetails(Event event, StringBuilder sb, PaypalResponseParser response, Boolean dual) throws UnsupportedEncodingException
    {
        final BigDecimal price = paymentAmount;
        final BigDecimal fee = dual ? price.multiply(new BigDecimal(percentage)).round(new MathContext(2)) : new BigDecimal(0);
        final BigDecimal providerPrice = price.subtract(fee);
        String paypalAccount = providerPaypalAccount;
        if (providerPrice.compareTo(new BigDecimal("3")) < 0)
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
        sb.append("&PAYMENTREQUEST_0_PAYMENTACTION=" + UriUtils.urlEncode("Sale"));
        sb.append("&PAYMENTREQUEST_0_DESC=" + UriUtils.urlEncode(StringUtils.getStringNotNullMaxLen(event.listing.title, 100)));

        //TODO important dont forget to switch it back
        Logger.info("paypal receiver " + event.user.account.paypalAccount);
        sb.append("&PAYMENTREQUEST_0_SELLERPAYPALACCOUNTID=" + event.user.account.paypalAccount);
        sb.append("&PAYMENTREQUEST_0_CURRENCYCODE=" + UriUtils.urlEncode(paymentCurrency));
        sb.append("&PAYMENTREQUEST_0_AMT=" + UriUtils.urlEncode(providerPrice.toPlainString()));
        sb.append("&PAYMENTREQUEST_0_PAYMENTREQUESTID=" + UriUtils.urlEncode(event.id + "_provider"));

        // item details
        sb.append("&L_PAYMENTREQUEST_0_NAME0=" + StringUtils.getStringNotNullMaxLen(event.listing.title, 100));
        sb.append("&L_PAYMENTREQUEST_0_AMT0=" + UriUtils.urlEncode(providerPrice.toPlainString()));

        if (dual)
        {
            Logger.info("paypal fee receiver " + providerPaypalAccount);

            // our payment
            sb.append("&PAYMENTREQUEST_1_PAYMENTACTION=" + UriUtils.urlEncode("Sale"));
            sb.append("&PAYMENTREQUEST_1_DESC=" + UriUtils.urlEncode(StringUtils.getStringNotNullMaxLen(event.listing.title, 100)));
            sb.append("&PAYMENTREQUEST_1_SELLERPAYPALACCOUNTID=" + providerPaypalAccount);
            sb.append("&PAYMENTREQUEST_1_CURRENCYCODE=" + UriUtils.urlEncode(paymentCurrency));
            sb.append("&PAYMENTREQUEST_1_AMT=" + UriUtils.urlEncode(fee.toPlainString()));
            sb.append("&PAYMENTREQUEST_1_PAYMENTREQUESTID=" + UriUtils.urlEncode(event.id + "_our"));

            // item details
            sb.append("&L_PAYMENTREQUEST_1_NAME0=" + "Servise provider fee");
            sb.append("&L_PAYMENTREQUEST_1_AMT0=" + UriUtils.urlEncode(fee.toPlainString()));
        }
    }

    private String executeRequest(StringBuilder sb)
    {
        try
        {
            HttpEntity entity = new StringEntity(sb.toString());
            HttpPost post = new HttpPost(paypalEndpoint);
            post.setEntity(entity);
            post.setHeader(new BasicHeader("Content-type", "application/x-www-form-urlencoded"));
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response = httpClient.execute(post);
            String resp = EntityUtils.toString(response.getEntity());
            return resp;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private String adaptiveExecuteRequest(List<Header> headers, StringBuilder sb, String endpoint) throws UnsupportedEncodingException, IOException, ClientProtocolException
    {
        HttpEntity entity = new StringEntity(sb.toString());
        HttpPost post = new HttpPost(endpoint);
        post.setEntity(entity);

        post.setHeaders(headers.toArray(new Header[headers.size()]));
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse resp = httpClient.execute(post);
        String response = EntityUtils.toString(resp.getEntity());
        return response;
    }

    private List<Header> adaptiveHeaders()
    {
        List<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("X-PAYPAL-SECURITY-USERID", BaseController.getProperty(BaseController.CONFIG_PAYPAL_USER)));
        headers.add(new BasicHeader("X-PAYPAL-SECURITY-PASSWORD", BaseController.getProperty(BaseController.CONFIG_PAYPAL_PWD)));
        headers.add(new BasicHeader("X-PAYPAL-SECURITY-SIGNATURE", BaseController.getProperty(BaseController.CONFIG_PAYPAL_SIGNATURE)));
        headers.add(new BasicHeader("X-PAYPAL-SECURITY-IPADDRESS", "127.0.0.1"));
        headers.add(new BasicHeader("X-PAYPAL-REQUEST-DATA-FORMAT", "NV"));
        headers.add(new BasicHeader("X-PAYPAL-RESPONSE-DATA-FORMAT", "NV"));
        headers.add(new BasicHeader("X-PAYPAL-APPLICATION-ID", BaseController.getProperty(BaseController.CONFIG_PAYPAL_ADAPTIVE_APPID)));
        return headers;
    }

    public class PaypalResponseParser
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
            Map<String, String> responseMap = parse(resp);
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
                errorMessage = UriUtils.urlDecode(errorMessage);
            }

            if (responseMap.containsKey("PAYMENTINFO_0_TRANSACTIONID"))
                transactionIdProvider = responseMap.get("PAYMENTINFO_0_TRANSACTIONID");

            if (responseMap.containsKey("PAYMENTINFO_1_TRANSACTIONID"))
                transactionIdOur = responseMap.get("PAYMENTINFO_1_TRANSACTIONID");
        }

        public Map<String, String> parse(String resp)
        {
            String[] pairs = resp.split("&");
            Map<String, String> responseMap = new LinkedHashMap<String, String>();
            for (int i = 0; i < pairs.length; i++)
            {
                String[] pair = pairs[i].split("=");
                responseMap.put(UriUtils.urlDecode(pair[0]), UriUtils.urlDecode(pair[1]));
            }
            return responseMap;
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
            this.token = UriUtils.urlDecode(token);
        }

        public Date getValidity()
        {
            return validity;
        }

        public void setValidity(String validity)
        {
            DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_PAYPAL);
            validity = UriUtils.urlDecode(validity);
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

}
