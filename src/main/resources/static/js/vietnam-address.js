// Vietnam Address API Integration
class VietnamAddress {
    constructor() {
        this.provinceSelect = document.getElementById('provinceSelect');
        this.districtSelect = document.getElementById('districtSelect');
        this.wardSelect = document.getElementById('wardSelect');
        this.specificAddress = document.getElementById('specificAddress');
        this.fullAddress = document.getElementById('fullAddress');
        
        this.init();
    }
    
    init() {
        this.loadProvinces();
        this.bindEvents();
    }
    
    bindEvents() {
        if (this.provinceSelect) {
            this.provinceSelect.addEventListener('change', (e) => {
                this.onProvinceChange(e.target.value);
            });
        }
        
        if (this.districtSelect) {
            this.districtSelect.addEventListener('change', (e) => {
                this.onDistrictChange(e.target.value);
            });
        }
        
        if (this.wardSelect) {
            this.wardSelect.addEventListener('change', () => {
                this.updateFullAddress();
            });
        }
        
        if (this.specificAddress) {
            this.specificAddress.addEventListener('input', () => {
                this.updateFullAddress();
            });
        }
    }
    
    async loadProvinces() {
        try {
            const response = await fetch('/api/address/provinces');
            const provinces = await response.json();
            
            this.provinceSelect.innerHTML = '<option value="">-- Chọn Tỉnh/Thành phố --</option>';
            
            provinces.forEach(province => {
                const option = document.createElement('option');
                option.value = province.code;
                option.textContent = province.name;
                option.dataset.name = province.name;
                this.provinceSelect.appendChild(option);
            });
        } catch (error) {
            console.error('Error loading provinces:', error);
        }
    }
    
    async onProvinceChange(provinceCode) {
        this.districtSelect.innerHTML = '<option value="">-- Chọn Quận/Huyện --</option>';
        this.wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
        this.districtSelect.disabled = true;
        this.wardSelect.disabled = true;
        
        if (!provinceCode) {
            this.updateFullAddress();
            return;
        }
        
        try {
            const response = await fetch(`/api/address/districts/${provinceCode}`);
            const data = await response.json();
            
            if (data.districts) {
                data.districts.forEach(district => {
                    const option = document.createElement('option');
                    option.value = district.code;
                    option.textContent = district.name;
                    option.dataset.name = district.name;
                    this.districtSelect.appendChild(option);
                });
                
                this.districtSelect.disabled = false;
            }
        } catch (error) {
            console.error('Error loading districts:', error);
        }
        
        this.updateFullAddress();
    }
    
    async onDistrictChange(districtCode) {
        this.wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
        this.wardSelect.disabled = true;
        
        if (!districtCode) {
            this.updateFullAddress();
            return;
        }
        
        try {
            const response = await fetch(`/api/address/wards/${districtCode}`);
            const data = await response.json();
            
            if (data.wards) {
                data.wards.forEach(ward => {
                    const option = document.createElement('option');
                    option.value = ward.code;
                    option.textContent = ward.name;
                    option.dataset.name = ward.name;
                    this.wardSelect.appendChild(option);
                });
                
                this.wardSelect.disabled = false;
            }
        } catch (error) {
            console.error('Error loading wards:', error);
        }
        
        this.updateFullAddress();
    }
    
    updateFullAddress() {
        const specificAddr = this.specificAddress?.value || '';
        const wardName = this.wardSelect?.selectedOptions[0]?.dataset.name || '';
        const districtName = this.districtSelect?.selectedOptions[0]?.dataset.name || '';
        const provinceName = this.provinceSelect?.selectedOptions[0]?.dataset.name || '';
        
        const addressParts = [specificAddr, wardName, districtName, provinceName].filter(part => part.trim());
        const fullAddress = addressParts.join(', ');
        
        if (this.fullAddress) {
            this.fullAddress.value = fullAddress;
        }
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new VietnamAddress();
});