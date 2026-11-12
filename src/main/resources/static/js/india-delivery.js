(function () {
    "use strict";

    /**
     * Binds cascading Indian State -> City dropdowns with custom-city fallback.
     * Elements use data attributes or explicit selectors passed in opts.
     */
    window.initIndiaLocationSelector = function (opts) {
        var stateSelect = document.getElementById(opts.stateSelectId);
        var citySelect = document.getElementById(opts.citySelectId);
        var customCityWrap = opts.customCityWrapId ? document.getElementById(opts.customCityWrapId) : null;
        var customCityInput = opts.customCityInputId ? document.getElementById(opts.customCityInputId) : null;
        var hiddenCityInput = opts.hiddenCityInputId ? document.getElementById(opts.hiddenCityInputId) : null;
        var stateCitiesMap = opts.stateCitiesMap || {};

        if (!stateSelect || !citySelect) {
            return { setLocation: function () {} };
        }

        function syncCityValue() {
            if (!hiddenCityInput) return;
            if (citySelect.value === "__CUSTOM__") {
                hiddenCityInput.value = customCityInput ? customCityInput.value.trim() : "";
            } else {
                hiddenCityInput.value = citySelect.value;
            }
        }

        function toggleCustomInput(show) {
            if (!customCityWrap) return;
            customCityWrap.style.display = show ? "block" : "none";
            if (customCityInput) {
                customCityInput.required = show && !citySelect.disabled;
                if (!show) {
                    customCityInput.value = "";
                }
            }
        }

        function populateCitiesForState(selectedState, targetCity) {
            var presetCities = stateCitiesMap[selectedState] || [];
            var previousCity = targetCity !== undefined ? targetCity : (hiddenCityInput ? hiddenCityInput.value : citySelect.value);

            citySelect.innerHTML = "";
            var defaultOpt = document.createElement("option");
            defaultOpt.value = "";
            defaultOpt.textContent = selectedState ? "-- Select City --" : "-- Select State First --";
            citySelect.appendChild(defaultOpt);

            var matchedPreset = false;
            presetCities.forEach(function (cityName) {
                var option = document.createElement("option");
                option.value = cityName;
                option.textContent = cityName;
                if (previousCity && previousCity.toLowerCase() === cityName.toLowerCase()) {
                    option.selected = true;
                    matchedPreset = true;
                }
                citySelect.appendChild(option);
            });

            if (selectedState) {
                var customOpt = document.createElement("option");
                customOpt.value = "__CUSTOM__";
                customOpt.textContent = "Other Indian City (Specify)";
                if (previousCity && !matchedPreset) {
                    customOpt.selected = true;
                    citySelect.appendChild(customOpt);
                    toggleCustomInput(true);
                    if (customCityInput) {
                        customCityInput.value = previousCity;
                    }
                } else {
                    citySelect.appendChild(customOpt);
                    toggleCustomInput(false);
                }
            } else {
                toggleCustomInput(false);
            }
            syncCityValue();
        }

        stateSelect.addEventListener("change", function () {
            var st = stateSelect.value;
            if (hiddenCityInput) {
                hiddenCityInput.value = "";
            }
            if (st && stateCitiesMap[st]) {
                populateCitiesForState(st, "");
            } else if (st) {
                fetch("/api/locations/cities?state=" + encodeURIComponent(st))
                    .then(function (r) { return r.ok ? r.json() : []; })
                    .then(function (cities) {
                        stateCitiesMap[st] = Array.isArray(cities) ? cities : [];
                        populateCitiesForState(st, "");
                    })
                    .catch(function () {
                        populateCitiesForState(st, "");
                    });
            } else {
                populateCitiesForState("", "");
            }
        });

        citySelect.addEventListener("change", function () {
            var isCustom = citySelect.value === "__CUSTOM__";
            toggleCustomInput(isCustom);
            syncCityValue();
        });

        if (customCityInput) {
            customCityInput.addEventListener("input", syncCityValue);
        }

        populateCitiesForState(stateSelect.value, opts.initialCity || (hiddenCityInput ? hiddenCityInput.value : ""));

        return {
            setLocation: function (state, city) {
                stateSelect.value = state || "";
                populateCitiesForState(stateSelect.value, city || "");
            },
            sync: syncCityValue
        };
    };

    /**
     * Initializes Checkout Delivery Method switcher (Home Delivery vs Physical Store Pickup),
     * Saved vs New Address toggle, input enabling/disabling so hidden forms don't block submission,
     * and live Order Summary total recalculation.
     */
    window.initCheckoutDeliveryUI = function (config) {
        var homeCard = document.getElementById("cardHomeDelivery");
        var pickupCard = document.getElementById("cardStorePickup");
        var methodHomeRadio = document.getElementById("radioDeliveryHome");
        var methodPickupRadio = document.getElementById("radioDeliveryPickup");

        var homePanel = document.getElementById("homeDeliveryPanel");
        var pickupPanel = document.getElementById("storePickupPanel");

        var addressModeInput = document.getElementById("addressSelectionModeInput");
        var newAddressBox = document.getElementById("newAddressFieldsContainer");
        var summaryMethodLabel = document.getElementById("summaryDeliveryMethod");
        var summaryFeeEl = document.getElementById("summaryDeliveryFee");
        var summaryTotalEl = document.getElementById("summaryGrandTotal");
        var quantityInput = document.getElementById("checkoutQuantityInput");

        var homeFee = Number(config.homeDeliveryCharge || 0);
        var pickupFee = Number(config.storePickupCharge || 0);
        var hasSavedAddresses = Boolean(config.hasSavedAddresses);

        function formatInr(amount) {
            return "₹" + Number(amount || 0).toFixed(2);
        }

        function setContainerDisabled(container, disabled) {
            if (!container) return;
            var controls = container.querySelectorAll("input, select, textarea");
            controls.forEach(function (el) {
                el.disabled = disabled;
            });
        }

        function syncNewAddressSection() {
            if (!homePanel || !newAddressBox) return;
            var isHomeSelected = methodHomeRadio && methodHomeRadio.checked;
            var useNew = !hasSavedAddresses;
            var savedRadios = document.querySelectorAll("input[name='selectedAddressId']");
            savedRadios.forEach(function (r) {
                var card = r.closest(".selection-radio-card");
                if (card) {
                    card.classList.toggle("is-selected", r.checked);
                }
                if (r.checked && r.value === "NEW") {
                    useNew = true;
                }
            });

            if (addressModeInput) {
                addressModeInput.value = useNew ? "NEW" : "SAVED";
            }

            if (isHomeSelected && useNew) {
                newAddressBox.style.display = "block";
                setContainerDisabled(newAddressBox, false);
            } else {
                newAddressBox.style.display = "none";
                setContainerDisabled(newAddressBox, true);
            }
        }

        function syncStoreSelectionHighlight() {
            var storeRadios = document.querySelectorAll("input[name='selectedStoreId']");
            storeRadios.forEach(function (r) {
                var card = r.closest(".selection-radio-card");
                if (card) {
                    card.classList.toggle("is-selected", r.checked);
                }
            });
        }

        function computeItemsSubtotal() {
            if (quantityInput && config.unitPrice != null) {
                var q = Math.max(1, parseInt(quantityInput.value || "1", 10) || 1);
                return Number(config.unitPrice) * q;
            }
            return Number(config.itemsSubtotal || 0);
        }

        function updateTotalsAndVisibility() {
            var isPickup = methodPickupRadio && methodPickupRadio.checked;
            var isHome = !isPickup;

            if (homeCard) homeCard.classList.toggle("is-selected", isHome);
            if (pickupCard) pickupCard.classList.toggle("is-selected", isPickup);

            if (homePanel) {
                homePanel.style.display = isHome ? "block" : "none";
                setContainerDisabled(homePanel, !isHome);
            }
            if (pickupPanel) {
                pickupPanel.style.display = isPickup ? "block" : "none";
                setContainerDisabled(pickupPanel, !isPickup);
            }

            syncNewAddressSection();
            syncStoreSelectionHighlight();

            var sub = computeItemsSubtotal();
            var fee = isPickup ? pickupFee : homeFee;
            var grandTotal = Math.max(0, sub + fee);

            var subtotalDisplay = document.getElementById("summaryItemsSubtotal");
            if (subtotalDisplay) {
                subtotalDisplay.textContent = formatInr(sub);
            }
            if (summaryMethodLabel) {
                summaryMethodLabel.textContent = isPickup ? "Physical Store Pickup" : "Home Delivery";
            }
            if (summaryFeeEl) {
                if (fee > 0) {
                    summaryFeeEl.textContent = formatInr(fee);
                    summaryFeeEl.className = "fw-bold text-dark";
                } else {
                    summaryFeeEl.textContent = isPickup ? "FREE (Store Pickup)" : "FREE";
                    summaryFeeEl.className = "text-success fw-bold";
                }
            }
            if (summaryTotalEl) {
                summaryTotalEl.textContent = formatInr(grandTotal);
            }
        }

        if (methodHomeRadio) {
            methodHomeRadio.addEventListener("change", updateTotalsAndVisibility);
        }
        if (methodPickupRadio) {
            methodPickupRadio.addEventListener("change", updateTotalsAndVisibility);
        }

        document.querySelectorAll("input[name='selectedAddressId']").forEach(function (radio) {
            radio.addEventListener("change", syncNewAddressSection);
        });
        document.querySelectorAll("input[name='selectedStoreId']").forEach(function (radio) {
            radio.addEventListener("change", syncStoreSelectionHighlight);
        });
        if (quantityInput) {
            quantityInput.addEventListener("input", updateTotalsAndVisibility);
            quantityInput.addEventListener("change", updateTotalsAndVisibility);
        }

        updateTotalsAndVisibility();
    };
})();

