/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.gui;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The game refuses to start if its screen files don't validate against
 * Nifty's schema (Main validates them), so check them here without starting
 * it
 */
class InterfaceXmlTest {

    @Test
    void testInterfaceXmlValidates() throws IOException, SAXException {
        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(InterfaceXmlTest.class.getResource("/nifty.xsd"));

        for (String file : new String[]{"/Interface/MainMenu.xml", "/Interface/GameHUD.xml"}) {
            try (InputStream in = InterfaceXmlTest.class.getResourceAsStream(file)) {
                assertNotNull(in, file);
                schema.newValidator().validate(new StreamSource(in));
            } catch (SAXException ex) {
                fail(file + " doesn't validate: " + ex.getMessage());
            }
        }
    }

}
