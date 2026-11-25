package com.aomanagement.service;

import com.aomanagement.dto.PartnerDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ConflictException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.PartnerCompanyRepository;
import com.aomanagement.repository.TenderCompanyParticipationRepository;
import com.aomanagement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerService {
    private final PartnerCompanyRepository partnerRepository;
    private final TenderRepository tenderRepository;
    private final TenderCompanyParticipationRepository participationRepository;

    @Transactional(readOnly = true)
    public List<PartnerDtos.PartnerResponse> all(String search) {
        List<PartnerCompany> partners = search == null || search.isBlank() ? partnerRepository.findAllByOrderByNameAsc() : partnerRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search);
        return partners.stream().map(this::toPartner).toList();
    }

    @Transactional(readOnly = true)
    public PartnerDtos.PartnerResponse one(Long id) {
        return toPartner(findPartner(id));
    }

    @Transactional
    public PartnerDtos.PartnerResponse create(PartnerDtos.PartnerRequest request) {
        PartnerCompany partner = apply(new PartnerCompany(), request);
        return toPartner(partnerRepository.save(partner));
    }

    @Transactional
    public PartnerDtos.PartnerResponse update(Long id, PartnerDtos.PartnerRequest request) {
        PartnerCompany partner = apply(findPartner(id), request);
        return toPartner(partner);
    }

    @Transactional
    public PartnerDtos.PartnerResponse status(Long id, PartnerDtos.PartnerStatusRequest request) {
        PartnerCompany partner = findPartner(id);
        partner.setActive(request.active());
        return toPartner(partner);
    }

    @Transactional
    public void delete(Long id) {
        PartnerCompany partner = findPartner(id);
        long participationCount = participationRepository.countByPartnerCompanyId(id);
        if (participationCount > 0) {
            throw new ConflictException("Partenaire lie a des appels d'offres");
        }
        partnerRepository.delete(partner);
    }

    @Transactional(readOnly = true)
    public PartnerDtos.ParticipationListResponse tenderParticipations(Long tenderId) {
        return new PartnerDtos.ParticipationListResponse(participationRepository.findByTenderIdOrderByLeadCompanyDescIdAsc(tenderId).stream().map(this::toParticipation).toList());
    }

    @Transactional
    public PartnerDtos.ParticipationResponse addParticipation(Long tenderId, PartnerDtos.ParticipationRequest request) {
        Tender tender = tenderRepository.findById(tenderId).orElseThrow(() -> new NotFoundException("Appel d'offres introuvable"));
        validateLead(tenderId, null, request.leadCompany());
        TenderCompanyParticipation p = new TenderCompanyParticipation();
        p.setTender(tender);
        applyParticipation(p, request);
        validateShare(tenderId, null, request.sharePercentage());
        tender.setParticipationMode(ParticipationMode.WITH_PARTNERS);
        return toParticipation(participationRepository.save(p));
    }

    @Transactional
    public void removeParticipation(Long tenderId, Long participationId) {
        TenderCompanyParticipation p = participationRepository.findById(participationId).orElseThrow(() -> new NotFoundException("Participant introuvable"));
        if (!p.getTender().getId().equals(tenderId)) throw new NotFoundException("Participant introuvable");
        participationRepository.delete(p);
    }

    private PartnerCompany apply(PartnerCompany partner, PartnerDtos.PartnerRequest request) {
        partner.setName(request.name());
        partner.setTradeName(request.tradeName());
        partner.setIce(request.ice());
        partner.setRc(request.rc());
        partner.setIfNumber(request.ifNumber());
        partner.setAddress(request.address());
        partner.setCity(request.city());
        partner.setCountry(request.country());
        partner.setEmail(request.email());
        partner.setPhone(request.phone());
        partner.setContactPerson(request.contactPerson());
        partner.setActive(request.active() == null || request.active());
        partner.setNotes(request.notes());
        return partner;
    }

    private void applyParticipation(TenderCompanyParticipation p, PartnerDtos.ParticipationRequest request) {
        p.setOurCompany(request.ourCompany());
        p.setPartnerCompany(request.ourCompany() ? null : findPartner(request.partnerCompanyId()));
        p.setParticipationType(request.participationType());
        p.setLeadCompany(request.leadCompany());
        p.setSharePercentage(request.sharePercentage());
        p.setRoleDescription(request.roleDescription());
    }

    private void validateLead(Long tenderId, Long participationId, boolean lead) {
        if (lead && (participationId == null ? participationRepository.existsByTenderIdAndLeadCompanyTrue(tenderId) : participationRepository.existsByTenderIdAndLeadCompanyTrueAndIdNot(tenderId, participationId))) {
            throw new ConflictException("Une seule societe peut etre chef de file");
        }
    }

    private void validateShare(Long tenderId, Long ignoreId, BigDecimal added) {
        if (added == null) return;
        BigDecimal total = participationRepository.findByTenderIdOrderByLeadCompanyDescIdAsc(tenderId).stream()
                .filter(p -> ignoreId == null || !p.getId().equals(ignoreId))
                .map(TenderCompanyParticipation::getSharePercentage)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(added);
        if (total.compareTo(new BigDecimal("100")) > 0) throw new ConflictException("La repartition depasse 100%");
    }

    private PartnerDtos.PartnerResponse toPartner(PartnerCompany p) {
        return new PartnerDtos.PartnerResponse(p.getId(), p.getName(), p.getTradeName(), p.getIce(), p.getRc(), p.getIfNumber(), p.getAddress(), p.getCity(), p.getCountry(), p.getEmail(), p.getPhone(), p.getContactPerson(), p.isActive(), p.getNotes(), participationRepository.countByPartnerCompanyId(p.getId()), p.getCreatedAt(), p.getUpdatedAt());
    }

    private PartnerDtos.ParticipationResponse toParticipation(TenderCompanyParticipation p) {
        String name = p.isOurCompany() ? "Notre societe" : p.getPartnerCompany().getName();
        return new PartnerDtos.ParticipationResponse(p.getId(), p.getPartnerCompany() == null ? null : p.getPartnerCompany().getId(), name, p.getParticipationType(), p.isOurCompany(), p.isLeadCompany(), p.getSharePercentage(), p.getRoleDescription());
    }

    private PartnerCompany findPartner(Long id) { return partnerRepository.findById(id).orElseThrow(() -> new NotFoundException("Partenaire introuvable")); }
}
