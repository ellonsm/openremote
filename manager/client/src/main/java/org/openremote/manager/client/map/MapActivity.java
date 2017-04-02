/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.map;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.assets.browser.TenantTreeNode;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.model.asset.Asset;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class MapActivity extends AssetBrowsingActivity<MapPlace> implements MapView.Presenter {

    final MapView view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;

    String assetId;
    Asset asset;
    String realm;

    @Inject
    public MapActivity(Environment environment,
                       AssetBrowser.Presenter assetBrowserPresenter,
                       MapView view,
                       AssetResource assetResource,
                       AssetMapper assetMapper,
                       MapResource mapResource,
                       JsonObjectMapper jsonObjectMapper) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.mapResource = mapResource;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    protected AppActivity<MapPlace> init(MapPlace place) {
        if (place instanceof MapAssetPlace) {
            MapAssetPlace mapAssetPlace = (MapAssetPlace) place;
            assetId = mapAssetPlace.getAssetId();
        } else if (place instanceof MapTenantPlace) {
            MapTenantPlace mapTenantPlace = (MapTenantPlace) place;
            realm = mapTenantPlace.getRealmId();
        }
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.getSelectedNode() instanceof TenantTreeNode) {
                environment.getPlaceController().goTo(
                    new MapTenantPlace(event.getSelectedNode().getId())
                );
            } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                environment.getPlaceController().goTo(
                    new MapAssetPlace(event.getSelectedNode().getId())
                );
            }
        }));

        if (!view.isMapInitialised()) {
            environment.getRequestService().execute(
                jsonObjectMapper,
                mapResource::getSettings,
                200,
                mapSettings -> {
                    view.initialiseMap(mapSettings);
                    if (asset != null) {
                        showAssetOnMap();
                    }
                },
                ex -> handleRequestException(ex, environment)
            );
        }

        hideAssetOnMap();

        asset = null;
        if (assetId != null) {
            assetBrowserPresenter.loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                if (asset != null) {
                    assetBrowserPresenter.selectAsset(asset);
                    showAssetOnMap();
                }
            });
        } else if (realm != null) {
            view.showInfo("TODO: Tenant map not implemented");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    protected void showAssetOnMap() {
        if (asset != null && asset.getCoordinates() != null) {
            if (asset.getCreatedOn() != null && asset.getCoordinates().length == 2) {
                // TODO: This assumes 0 is Lng and 1 is Lat, which is true for PostGIS backend
                // TODO: Because Lat/Lng is the 'right way', we flip it here for display
                // TODO: Rounding to 5 decimals gives us precision of about 1 meter, should be enough
                view.showInfo("Location: " + (
                        round(asset.getCoordinates()[1], 5) + " " + round(asset.getCoordinates()[0], 5) + " Lat|Lng"
                    )
                );
            } else {
                view.showInfo(null);
            }
            view.showFeaturesSelection(MapView.getFeature(asset));
            view.flyTo(asset.getCoordinates());
        } else {
            view.showInfo(null);
        }
    }

    protected void hideAssetOnMap() {
        view.hideFeaturesSelection();
    }

    protected String round(double d, int places) {
        return new BigDecimal(d).setScale(places, RoundingMode.HALF_UP).toString();
    }

}
