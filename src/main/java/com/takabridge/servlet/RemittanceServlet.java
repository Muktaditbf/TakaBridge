package com.takabridge.servlet;

import com.takabridge.model.Remittance;
import com.takabridge.service.ConversionService;
import com.takabridge.service.RemittanceCalculator;
import com.takabridge.service.ValidationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * The remittance calculator: how many taka the family at home receives after
 * the transfer fee and the government's 2.5% bonus, through each channel.
 *
 * The form uses GET because nothing is saved - the answer is only calculated -
 * so the address can be bookmarked or shared, e.g. /remittance?from=SAR&amount=2000
 */
@WebServlet("/remittance")
public class RemittanceServlet extends BaseServlet {

    private final ConversionService service = new ConversionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String from = request.getParameter("from");
        String amount = request.getParameter("amount");

        try {
            request.setAttribute("currencies", service.listCurrencies());

            // Only calculate once the form has been sent.
            if (amount != null) {
                List<Remittance> channels = service.remittance(from, amount);
                request.setAttribute("channels", channels);
                request.setAttribute("needsDocuments", service.remittanceNeedsDocuments(from, amount));
            }
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Could not read rates for the remittance calculator", e);
            request.setAttribute("dbError",
                    "TakaBridge cannot reach the database right now. "
                  + "Please check that Oracle is running and try again.");
        }

        request.setAttribute("formFrom", from == null ? "USD" : from);
        request.setAttribute("formAmount", amount == null ? "500" : amount);
        request.setAttribute("bonusPercent", RemittanceCalculator.GOVERNMENT_BONUS.movePointRight(2).stripTrailingZeros().toPlainString());
        request.setAttribute("documentsLimit", RemittanceCalculator.NO_DOCUMENTS_LIMIT_USD);

        render(request, response, "remittance.jsp", "remittance");
    }
}
